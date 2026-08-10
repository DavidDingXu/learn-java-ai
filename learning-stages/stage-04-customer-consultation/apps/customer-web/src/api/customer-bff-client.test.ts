import { describe, expect, it, vi } from "vitest";

import { CustomerBffApiError, HttpCustomerBffClient } from "./customer-bff-client";

function sseResponse(body: string): Response {
  const encoder = new TextEncoder();
  return new Response(new ReadableStream({
    start(controller) {
      controller.enqueue(encoder.encode(body));
      controller.close();
    },
  }), {
    status: 200,
    headers: { "Content-Type": "text/event-stream" },
  });
}

describe("HttpCustomerBffClient", () => {
  it("keeps the browser global as the receiver of the default fetch implementation", async () => {
    let receiver: unknown;
    const browserFetch = vi.fn(function (this: unknown) {
      receiver = this;
      return Promise.resolve(sseResponse(
        'event: completed\ndata: {"refused":false,"refusalReason":null}\n\n',
      ));
    });
    vi.stubGlobal("fetch", browserFetch);
    try {
      const client = new HttpCustomerBffClient({
        baseUrl: "",
        accessToken: () => "customer-token",
      });

      for await (const ignored of client.streamAnswer(
        { question: "多久到账？" },
        new AbortController().signal,
      )) {
        void ignored;
      }

      expect(receiver).toBe(globalThis);
    } finally {
      vi.unstubAllGlobals();
    }
  });

  it("streams a POST request with bearer identity and typed named events", async () => {
    const fetcher = vi.fn<typeof fetch>().mockResolvedValue(sseResponse([
      "event: session",
      'data: {"conversationId":"c-1","attemptId":"a-1","retryOfAttemptId":null}',
      "",
      "event: citation",
      'data: {"citation":{"documentId":"refund-policy","version":"1","sectionId":"10","title":"退款政策"}}',
      "",
      "event: completed",
      'data: {"refused":true,"refusalReason":"缺少退款审核状态"}',
      "",
      "",
    ].join("\n")));
    const client = new HttpCustomerBffClient({
      baseUrl: "",
      accessToken: () => "customer-token",
      fetcher,
    });

    const events = [];
    for await (const event of client.streamAnswer({ question: "多久到账？" }, new AbortController().signal)) {
      events.push(event);
    }

    expect(fetcher).toHaveBeenCalledWith("/api/v1/customer/consultations/answers/stream", expect.objectContaining({
      method: "POST",
      headers: {
        Accept: "text/event-stream",
        Authorization: "Bearer customer-token",
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ question: "多久到账？" }),
    }));
    expect(events.map((event) => event.type)).toEqual(["session", "citation", "completed"]);
    expect(events.at(-1)).toEqual({
      type: "completed",
      refused: true,
      refusalReason: "缺少退款审核状态",
    });
  });

  it("maps a JSON API error and falls back to the HTTP status for an empty response", async () => {
    const fetcher = vi.fn<typeof fetch>()
      .mockResolvedValueOnce(new Response(JSON.stringify({
        code: "CONSULTATION_RATE_LIMITED",
        message: "请求过于频繁",
      }), {
        status: 429,
        headers: { "Content-Type": "application/json" },
      }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));
    const client = new HttpCustomerBffClient({
      baseUrl: "https://customer.example.com",
      accessToken: () => "customer-token",
      fetcher,
    });

    await expect(client.retry("c-1", "a-1")).rejects.toEqual(expect.objectContaining({
      code: "CONSULTATION_RATE_LIMITED",
      status: 429,
    }));
    await expect(client.retry("c-1", "a-1")).rejects.toEqual(expect.objectContaining({
      code: "UNAUTHORIZED",
      status: 401,
    }));
    expect(CustomerBffApiError).toBeDefined();
  });

  it("rejects a completed event without explicit refusal fields", async () => {
    const client = new HttpCustomerBffClient({
      baseUrl: "",
      accessToken: () => "customer-token",
      fetcher: vi.fn<typeof fetch>().mockResolvedValue(sseResponse(
        'event: completed\ndata: {"refused":false}\n\n',
      )),
    });

    const consume = async () => {
      for await (const ignored of client.streamAnswer(
        { question: "多久到账？" },
        new AbortController().signal,
      )) {
        void ignored;
      }
    };

    await expect(consume()).rejects.toEqual(expect.objectContaining({
      code: "INVALID_STREAM_EVENT",
    }));
  });
});
