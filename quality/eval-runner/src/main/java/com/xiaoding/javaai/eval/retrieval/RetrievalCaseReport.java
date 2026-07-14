package com.xiaoding.javaai.eval.retrieval;

import java.util.List;

public record RetrievalCaseReport(
        String caseId,
        List<String> expectedChunkIds,
        List<String> retrievedChunkIds,
        long latencyMillis,
        String embeddingModel
) {
    public RetrievalCaseReport {
        expectedChunkIds = List.copyOf(expectedChunkIds);
        retrievedChunkIds = List.copyOf(retrievedChunkIds);
    }
}
