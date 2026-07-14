package com.xiaoding.javaai.knowledge.answer.application;

import java.util.List;

public record KnowledgeAnswer(
        String answer,
        List<Citation> citations,
        boolean refused,
        String refusalReason,
        String model,
        ModelUsage usage,
        String finishReason,
        String traceId
) {

    public KnowledgeAnswer {
        citations = List.copyOf(citations);
    }
}
