package com.xiaoding.javaai.customer.consultation.domain;

import java.util.List;

public record KnowledgeAnswerView(
        String answer,
        List<CitationView> citations,
        boolean refused,
        String refusalReason,
        String traceId
) {
    public KnowledgeAnswerView {
        answer = answer == null ? "" : answer.trim();
        citations = citations == null ? List.of() : List.copyOf(citations);
        traceId = requireText(traceId, "traceId");
        if (refused && (refusalReason == null || refusalReason.isBlank())) {
            throw new IllegalArgumentException("refusalReason is required for a refused answer");
        }
        if (!refused && answer.isBlank()) {
            throw new IllegalArgumentException("answer must not be blank when the request is answered");
        }
        refusalReason = refusalReason == null ? null : refusalReason.trim();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
