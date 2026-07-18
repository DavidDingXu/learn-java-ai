export interface Citation {
  documentId: string;
  version: string;
  sectionId: string;
  title: string;
}

export interface CustomerAnswer {
  conversationId: string;
  attemptId: string;
  retryOfAttemptId: string | null;
  answer: string;
  citations: Citation[];
  refused: boolean;
  refusalReason: string | null;
  traceId: string;
}

export interface FeedbackRequest {
  rating: "HELPFUL" | "NOT_HELPFUL";
  reasonCode?: string;
  comment?: string;
}

export interface TicketHandoffReceipt {
  taskId: string;
  status: "ACCEPTED";
  duplicate: boolean;
}

export type CustomerStreamEvent =
  | {
      type: "session";
      conversationId: string;
      attemptId: string;
      retryOfAttemptId: string | null;
    }
  | { type: "metadata"; traceId: string }
  | { type: "delta"; text: string }
  | { type: "heartbeat"; epochMillis: number }
  | { type: "citation"; citation: Citation }
  | { type: "completed" }
  | { type: "error"; code: string; message: string };
