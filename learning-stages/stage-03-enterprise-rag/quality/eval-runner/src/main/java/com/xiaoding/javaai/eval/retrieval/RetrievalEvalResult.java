package com.xiaoding.javaai.eval.retrieval;

import java.util.List;
import java.util.Set;

public record RetrievalEvalResult(
        String caseId,
        Set<String> expectedChunkIds,
        List<String> retrievedChunkIds
) {
    public RetrievalEvalResult {
        if (caseId == null || caseId.isBlank()) throw new IllegalArgumentException("caseId must not be blank");
        if (expectedChunkIds == null || expectedChunkIds.isEmpty()) {
            throw new IllegalArgumentException("expectedChunkIds must not be empty");
        }
        if (retrievedChunkIds == null) throw new IllegalArgumentException("retrievedChunkIds must not be null");
        expectedChunkIds = Set.copyOf(expectedChunkIds);
        retrievedChunkIds = List.copyOf(retrievedChunkIds);
    }
}
