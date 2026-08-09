package com.xiaoding.javaai.eval.model;

import java.util.List;

public record KnowledgeAnswerSnapshot(
        String answer,
        List<String> citationSectionIds,
        boolean refused,
        String refusalReason,
        String model,
        String traceId,
        int promptTokens,
        int completionTokens,
        int totalTokens
) {

    public KnowledgeAnswerSnapshot {
        citationSectionIds = citationSectionIds == null ? List.of() : List.copyOf(citationSectionIds);
    }
}
