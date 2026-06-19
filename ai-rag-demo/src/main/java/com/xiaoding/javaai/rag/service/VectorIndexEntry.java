package com.xiaoding.javaai.rag.service;

import java.util.List;

public record VectorIndexEntry(
        DocumentChunk chunk,
        List<Double> vector
) {
    public VectorIndexEntry {
        if (chunk == null) {
            throw new IllegalArgumentException("chunk must not be null");
        }
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        vector = List.copyOf(vector);
    }
}
