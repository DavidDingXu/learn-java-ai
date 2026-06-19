package com.xiaoding.javaai.eval.service;

import java.util.Set;

public record RagEvalCase(
        String caseId,
        String question,
        Set<String> expectedChunkIds,
        boolean expectNoEvidence
) {
    public RagEvalCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        expectedChunkIds = expectedChunkIds == null ? Set.of() : Set.copyOf(expectedChunkIds);
    }
}
