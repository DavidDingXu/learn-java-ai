package com.xiaoding.javaai.customer.consultation.application;

import com.xiaoding.javaai.customer.consultation.domain.CitationView;

import java.util.List;

public record CustomerAnswer(
        String conversationId,
        String attemptId,
        String retryOfAttemptId,
        String answer,
        List<CitationView> citations,
        boolean refused,
        String refusalReason,
        String traceId
) {
    public CustomerAnswer {
        citations = List.copyOf(citations);
    }
}
