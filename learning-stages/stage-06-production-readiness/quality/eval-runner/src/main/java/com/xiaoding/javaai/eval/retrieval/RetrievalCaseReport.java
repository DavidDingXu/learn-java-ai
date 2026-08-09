package com.xiaoding.javaai.eval.retrieval;

import java.util.List;
import java.util.Objects;

public record RetrievalCaseReport(
        String caseId,
        List<String> expectedChunkIds,
        List<String> retrievedChunkIds,
        long latencyMillis,
        String embeddingModel,
        RetrievalCaseStatus status,
        String error
) {
    public RetrievalCaseReport {
        expectedChunkIds = List.copyOf(expectedChunkIds);
        retrievedChunkIds = List.copyOf(retrievedChunkIds);
        status = Objects.requireNonNull(status, "status must not be null");
        if (latencyMillis < 0) throw new IllegalArgumentException("latencyMillis must not be negative");
        error = error == null || error.isBlank() ? null : error.strip();
        if (status == RetrievalCaseStatus.ERROR && error == null) {
            throw new IllegalArgumentException("error must describe a failed retrieval case");
        }
        if (status == RetrievalCaseStatus.COMPLETED && error != null) {
            throw new IllegalArgumentException("completed retrieval case must not contain an error");
        }
    }

    public RetrievalCaseReport(
            String caseId,
            List<String> expectedChunkIds,
            List<String> retrievedChunkIds,
            long latencyMillis,
            String embeddingModel
    ) {
        this(caseId, expectedChunkIds, retrievedChunkIds, latencyMillis,
                embeddingModel, RetrievalCaseStatus.COMPLETED, null);
    }
}
