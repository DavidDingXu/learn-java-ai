import type {
  Citation,
  CustomerAnswer,
  CustomerStreamEvent,
  FeedbackRequest,
  TicketHandoffReceipt,
} from "../../domain/consultation";

export type ExchangeStatus = "streaming" | "completed" | "cancelled" | "error";

export interface ConsultationExchange {
  localId: string;
  question: string;
  status: ExchangeStatus;
  conversationId: string | null;
  attemptId: string | null;
  retryOfAttemptId: string | null;
  answer: string;
  citations: Citation[];
  refused: boolean;
  refusalReason: string | null;
  traceId: string | null;
  error: string | null;
  feedback: FeedbackRequest["rating"] | null;
  handoff: TicketHandoffReceipt | null;
}

export interface ConsultationState {
  conversationId: string | null;
  exchanges: ConsultationExchange[];
}

export const initialConsultationState: ConsultationState = {
  conversationId: null,
  exchanges: [],
};

export type ConsultationAction =
  | { type: "stream-started"; localId: string; question: string }
  | { type: "stream-event"; localId: string; event: CustomerStreamEvent }
  | { type: "stream-failed"; localId: string; message: string }
  | { type: "stream-cancelled"; localId: string }
  | { type: "retry-succeeded"; localId: string; question: string; answer: CustomerAnswer }
  | { type: "feedback-recorded"; localId: string; rating: FeedbackRequest["rating"] }
  | { type: "handoff-accepted"; localId: string; receipt: TicketHandoffReceipt };

export function consultationReducer(
  state: ConsultationState,
  action: ConsultationAction,
): ConsultationState {
  if (action.type === "stream-started") {
    return {
      ...state,
      exchanges: [...state.exchanges, emptyExchange(action.localId, action.question)],
    };
  }
  if (action.type === "retry-succeeded") {
    return {
      conversationId: action.answer.conversationId,
      exchanges: [...state.exchanges, exchangeFromAnswer(action.localId, action.question, action.answer)],
    };
  }

  const exchange = state.exchanges.find((candidate) => candidate.localId === action.localId);
  if (!exchange) return state;

  if (action.type === "stream-event") {
    const nextExchange = reduceStreamEvent(exchange, action.event);
    return {
      conversationId: action.event.type === "session"
        ? action.event.conversationId
        : state.conversationId,
      exchanges: replaceExchange(state.exchanges, nextExchange),
    };
  }
  if (action.type === "stream-failed") {
    return updateExchange(state, { ...exchange, status: "error", error: action.message });
  }
  if (action.type === "stream-cancelled") {
    return updateExchange(state, {
      ...exchange,
      status: "cancelled",
      error: "已停止生成，可以重新提交原问题",
    });
  }
  if (action.type === "feedback-recorded") {
    return updateExchange(state, { ...exchange, feedback: action.rating });
  }
  return updateExchange(state, { ...exchange, handoff: action.receipt });
}

function reduceStreamEvent(
  exchange: ConsultationExchange,
  event: CustomerStreamEvent,
): ConsultationExchange {
  switch (event.type) {
    case "session":
      return {
        ...exchange,
        conversationId: event.conversationId,
        attemptId: event.attemptId,
        retryOfAttemptId: event.retryOfAttemptId,
      };
    case "metadata":
      return { ...exchange, traceId: event.traceId };
    case "delta":
      return { ...exchange, answer: `${exchange.answer}${event.text}` };
    case "citation":
      return {
        ...exchange,
        citations: exchange.citations.some((citation) => sameCitation(citation, event.citation))
          ? exchange.citations
          : [...exchange.citations, event.citation],
      };
    case "completed":
      return {
        ...exchange,
        status: "completed",
        refused: event.refused,
        refusalReason: event.refusalReason,
      };
    case "error":
      return { ...exchange, status: "error", error: event.message };
    case "heartbeat":
      return exchange;
  }
}

function emptyExchange(localId: string, question: string): ConsultationExchange {
  return {
    localId,
    question,
    status: "streaming",
    conversationId: null,
    attemptId: null,
    retryOfAttemptId: null,
    answer: "",
    citations: [],
    refused: false,
    refusalReason: null,
    traceId: null,
    error: null,
    feedback: null,
    handoff: null,
  };
}

function exchangeFromAnswer(
  localId: string,
  question: string,
  answer: CustomerAnswer,
): ConsultationExchange {
  return {
    localId,
    question,
    status: "completed",
    conversationId: answer.conversationId,
    attemptId: answer.attemptId,
    retryOfAttemptId: answer.retryOfAttemptId,
    answer: answer.answer,
    citations: answer.citations,
    refused: answer.refused,
    refusalReason: answer.refusalReason,
    traceId: answer.traceId,
    error: null,
    feedback: null,
    handoff: null,
  };
}

function sameCitation(left: Citation, right: Citation): boolean {
  return left.documentId === right.documentId
    && left.version === right.version
    && left.sectionId === right.sectionId;
}

function updateExchange(
  state: ConsultationState,
  exchange: ConsultationExchange,
): ConsultationState {
  return { ...state, exchanges: replaceExchange(state.exchanges, exchange) };
}

function replaceExchange(
  exchanges: ConsultationExchange[],
  replacement: ConsultationExchange,
): ConsultationExchange[] {
  return exchanges.map((exchange) => (
    exchange.localId === replacement.localId ? replacement : exchange
  ));
}
