package com.xiaoding.javaai.knowledge.retrieval.application;

import java.time.Instant;

public record RetrieveKnowledgeQuery(
        String question,
        KnowledgeAccessScope accessScope,
        Instant effectiveAt,
        int topK
) {
    public RetrieveKnowledgeQuery {
        if (question == null || question.isBlank()) throw new IllegalArgumentException("question must not be blank");
        if (effectiveAt == null) throw new IllegalArgumentException("effectiveAt must not be null");
        if (topK < 1 || topK > 100) throw new IllegalArgumentException("topK must be between 1 and 100");
    }
}
