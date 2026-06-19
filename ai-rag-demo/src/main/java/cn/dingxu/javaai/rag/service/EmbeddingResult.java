package cn.dingxu.javaai.rag.service;

import java.util.List;

public record EmbeddingResult(
        String documentId,
        String chunkId,
        String version,
        List<Double> vector,
        boolean cacheHit
) {
    public EmbeddingResult {
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (chunkId == null || chunkId.isBlank()) {
            throw new IllegalArgumentException("chunkId must not be blank");
        }
        version = version == null || version.isBlank() ? "v1" : version;
        if (vector == null || vector.isEmpty()) {
            throw new IllegalArgumentException("vector must not be empty");
        }
        vector = List.copyOf(vector);
    }
}
