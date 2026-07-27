import type {
  Citation,
  CustomerAnswer,
  CustomerStreamEvent,
  FeedbackRequest,
  TicketHandoffReceipt,
} from "../domain/consultation";
import { parseSseStream } from "./sse";

export interface StreamAnswerRequest {
  conversationId?: string;
  question: string;
}

export interface CustomerBffClient {
  streamAnswer(
    request: StreamAnswerRequest,
    signal: AbortSignal,
  ): AsyncGenerator<CustomerStreamEvent>;
  recordFeedback(
    conversationId: string,
    attemptId: string,
    request: FeedbackRequest,
  ): Promise<void>;
  retry(conversationId: string, attemptId: string): Promise<CustomerAnswer>;
  handoff(
    conversationId: string,
    attemptId: string,
    reasonCode: string,
  ): Promise<TicketHandoffReceipt>;
}

export class CustomerBffApiError extends Error {
  constructor(
    readonly code: string,
    message: string,
    readonly status: number,
    options?: ErrorOptions,
  ) {
    super(message, options);
    this.name = "CustomerBffApiError";
  }
}

interface ClientOptions {
  baseUrl: string;
  accessToken: () => string | null;
  fetcher?: typeof fetch;
}

const ERROR_CODE_BY_STATUS: Readonly<Record<number, string>> = {
  400: "INVALID_REQUEST",
  401: "UNAUTHORIZED",
  403: "FORBIDDEN",
  409: "CONSULTATION_STATE_CONFLICT",
  429: "CONSULTATION_RATE_LIMITED",
  502: "DOWNSTREAM_SERVICE_FAILED",
};

export class HttpCustomerBffClient implements CustomerBffClient {
  private readonly baseUrl: string;
  private readonly fetcher: typeof fetch;

  constructor(private readonly options: ClientOptions) {
    this.baseUrl = options.baseUrl.replace(/\/$/, "");
    this.fetcher = options.fetcher ?? globalThis.fetch.bind(globalThis);
  }

  async *streamAnswer(
    request: StreamAnswerRequest,
    signal: AbortSignal,
  ): AsyncGenerator<CustomerStreamEvent> {
    const response = await this.fetcher(this.url("/api/v1/customer/consultations/answers/stream"), {
      method: "POST",
      headers: this.headers("text/event-stream"),
      body: JSON.stringify({
        ...(request.conversationId ? { conversationId: request.conversationId } : {}),
        question: request.question,
      }),
      signal,
    });
    await ensureSuccess(response);
    const contentType = response.headers.get("Content-Type") ?? "";
    if (!contentType.toLowerCase().startsWith("text/event-stream") || !response.body) {
      throw new CustomerBffApiError(
        "INVALID_STREAM_RESPONSE",
        "咨询服务没有返回事件流",
        response.status,
      );
    }

    let terminalEventReceived = false;
    for await (const event of parseSseStream(response.body)) {
      const mapped = mapStreamEvent(event.event, event.data);
      yield mapped;
      if (mapped.type === "completed" || mapped.type === "error") {
        terminalEventReceived = true;
        break;
      }
    }
    if (!terminalEventReceived && !signal.aborted) {
      throw new CustomerBffApiError(
        "STREAM_INTERRUPTED",
        "回答连接提前中断，请重新提交问题",
        0,
      );
    }
  }

  async recordFeedback(
    conversationId: string,
    attemptId: string,
    request: FeedbackRequest,
  ): Promise<void> {
    const response = await this.fetcher(this.attemptUrl(conversationId, attemptId, "/feedback"), {
      method: "PUT",
      headers: this.headers("application/json"),
      body: JSON.stringify(request),
    });
    await ensureSuccess(response);
  }

  async retry(conversationId: string, attemptId: string): Promise<CustomerAnswer> {
    const response = await this.fetcher(this.attemptUrl(conversationId, attemptId, "/retry"), {
      method: "POST",
      headers: this.headers("application/json"),
    });
    await ensureSuccess(response);
    return readJson<CustomerAnswer>(response, "INVALID_ANSWER_RESPONSE");
  }

  async handoff(
    conversationId: string,
    attemptId: string,
    reasonCode: string,
  ): Promise<TicketHandoffReceipt> {
    const response = await this.fetcher(this.attemptUrl(conversationId, attemptId, "/handoffs"), {
      method: "POST",
      headers: this.headers("application/json"),
      body: JSON.stringify({ reasonCode }),
    });
    await ensureSuccess(response);
    return readJson<TicketHandoffReceipt>(response, "INVALID_HANDOFF_RESPONSE");
  }

  private headers(accept: string): Record<string, string> {
    const token = this.options.accessToken();
    if (!token?.trim()) {
      throw new CustomerBffApiError("AUTH_REQUIRED", "登录状态已失效", 401);
    }
    return {
      Accept: accept,
      Authorization: `Bearer ${token.trim()}`,
      "Content-Type": "application/json",
    };
  }

  private url(path: string): string {
    return `${this.baseUrl}${path}`;
  }

  private attemptUrl(
    conversationId: string,
    attemptId: string,
    suffix: string,
  ): string {
    return this.url(
      `/api/v1/customer/consultations/${encodeURIComponent(conversationId)}`
      + `/attempts/${encodeURIComponent(attemptId)}${suffix}`,
    );
  }
}

async function ensureSuccess(response: Response): Promise<void> {
  if (response.ok) return;
  const contentType = response.headers.get("Content-Type") ?? "";
  let code = ERROR_CODE_BY_STATUS[response.status] ?? "REQUEST_FAILED";
  let message = response.statusText || "请求失败，请稍后重试";

  if (contentType.toLowerCase().includes("json")) {
    try {
      const body = await response.json() as { code?: unknown; message?: unknown };
      if (typeof body.code === "string" && body.code) code = body.code;
      if (typeof body.message === "string" && body.message) message = body.message;
    } catch {
      // The status remains the reliable fallback when a proxy returns malformed JSON.
    }
  }
  throw new CustomerBffApiError(code, message, response.status);
}

async function readJson<T>(response: Response, code: string): Promise<T> {
  try {
    return await response.json() as T;
  } catch (error) {
    throw new CustomerBffApiError(code, "服务返回了无法识别的数据", response.status, { cause: error });
  }
}

function mapStreamEvent(event: string, data: unknown): CustomerStreamEvent {
  const payload = objectPayload(event, data);
  switch (event) {
    case "session":
      return {
        type: "session",
        conversationId: stringField(event, payload, "conversationId"),
        attemptId: stringField(event, payload, "attemptId"),
        retryOfAttemptId: nullableStringField(event, payload, "retryOfAttemptId"),
      };
    case "metadata":
      return { type: "metadata", traceId: stringField(event, payload, "traceId") };
    case "delta":
      return { type: "delta", text: stringField(event, payload, "text") };
    case "heartbeat":
      return { type: "heartbeat", epochMillis: numberField(event, payload, "epochMillis") };
    case "citation":
      return { type: "citation", citation: citationField(event, payload.citation) };
    case "completed":
      return completedEvent(event, payload);
    case "error":
      return {
        type: "error",
        code: stringField(event, payload, "code"),
        message: stringField(event, payload, "message"),
      };
    default:
      throw invalidEvent(event, "unsupported event name");
  }
}

function objectPayload(event: string, value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== "object" || Array.isArray(value)) {
    throw invalidEvent(event, "data must be a JSON object");
  }
  return value as Record<string, unknown>;
}

function stringField(event: string, payload: Record<string, unknown>, field: string): string {
  const value = payload[field];
  if (typeof value !== "string" || !value) throw invalidEvent(event, `${field} must be a string`);
  return value;
}

function nullableStringField(
  event: string,
  payload: Record<string, unknown>,
  field: string,
): string | null {
  const value = payload[field];
  if (value === null || value === undefined) return null;
  if (typeof value !== "string") throw invalidEvent(event, `${field} must be a string or null`);
  return value;
}

function numberField(event: string, payload: Record<string, unknown>, field: string): number {
  const value = payload[field];
  if (typeof value !== "number" || !Number.isFinite(value)) {
    throw invalidEvent(event, `${field} must be a number`);
  }
  return value;
}

function booleanField(event: string, payload: Record<string, unknown>, field: string): boolean {
  const value = payload[field];
  if (typeof value !== "boolean") throw invalidEvent(event, `${field} must be a boolean`);
  return value;
}

function completedEvent(
  event: string,
  payload: Record<string, unknown>,
): Extract<CustomerStreamEvent, { type: "completed" }> {
  const refused = booleanField(event, payload, "refused");
  if (!("refusalReason" in payload)) {
    throw invalidEvent(event, "refusalReason must be present");
  }
  const refusalReason = nullableStringField(event, payload, "refusalReason");
  if (refused && !refusalReason?.trim()) {
    throw invalidEvent(event, "refusalReason is required when refused is true");
  }
  return { type: "completed", refused, refusalReason };
}

function citationField(event: string, value: unknown): Citation {
  const payload = objectPayload(event, value);
  return {
    documentId: stringField(event, payload, "documentId"),
    version: stringField(event, payload, "version"),
    sectionId: stringField(event, payload, "sectionId"),
    title: stringField(event, payload, "title"),
  };
}

function invalidEvent(event: string, reason: string): CustomerBffApiError {
  return new CustomerBffApiError(
    "INVALID_STREAM_EVENT",
    `无法识别 ${event} 事件：${reason}`,
    0,
  );
}
