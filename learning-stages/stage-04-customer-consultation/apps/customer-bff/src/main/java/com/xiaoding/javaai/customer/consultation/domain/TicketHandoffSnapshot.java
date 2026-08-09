package com.xiaoding.javaai.customer.consultation.domain;

import java.time.Instant;
import java.util.List;

public record TicketHandoffSnapshot(
        String conversationId,
        String attemptId,
        String tenantId,
        String customerId,
        String question,
        String answer,
        List<CitationView> citations,
        String refusalReason,
        AnswerFeedback feedback,
        String conversationSummary,
        String reasonCode,
        String idempotencyKey,
        String sourceTraceId,
        Instant createdAt
) {
    public TicketHandoffSnapshot {
        citations = List.copyOf(citations);
        conversationSummary = conversationSummary == null ? "" : conversationSummary;
    }
}
