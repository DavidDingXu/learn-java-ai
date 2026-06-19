package com.xiaoding.javaai.rag.service;

import java.util.Set;

public record HybridRetrievalResult(
        DocumentChunk chunk,
        double score,
        Set<String> sources
) {
    public HybridRetrievalResult {
        if (chunk == null) {
            throw new IllegalArgumentException("chunk must not be null");
        }
        sources = sources == null ? Set.of() : Set.copyOf(sources);
    }
}
