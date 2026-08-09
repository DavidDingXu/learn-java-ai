package com.xiaoding.javaai.eval.retrieval;

import java.util.List;

public record RetrievalClientResult(
        String embeddingModel,
        List<String> chunkIds,
        long latencyMillis
) {
    public RetrievalClientResult {
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        if (chunkIds == null) throw new IllegalArgumentException("chunkIds must not be null");
        if (latencyMillis < 0) throw new IllegalArgumentException("latencyMillis must not be negative");
        chunkIds = List.copyOf(chunkIds);
    }
}
