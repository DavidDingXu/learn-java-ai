package com.xiaoding.javaai.eval.retrieval;

import java.util.Set;

public record RetrievalEvalCase(
        String id,
        String question,
        Set<String> expectedChunkIds
) {
    public RetrievalEvalCase {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id must not be blank");
        if (question == null || question.isBlank()) throw new IllegalArgumentException("question must not be blank");
        if (expectedChunkIds == null || expectedChunkIds.isEmpty()) {
            throw new IllegalArgumentException("expectedChunkIds must not be empty");
        }
        expectedChunkIds = Set.copyOf(expectedChunkIds);
        if (expectedChunkIds.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException("expectedChunkIds must not contain blank values");
        }
    }
}
