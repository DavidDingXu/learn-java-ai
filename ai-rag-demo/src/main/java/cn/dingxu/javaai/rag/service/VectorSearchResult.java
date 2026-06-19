package cn.dingxu.javaai.rag.service;

public record VectorSearchResult(
        DocumentChunk chunk,
        double score
) {
    public VectorSearchResult {
        if (chunk == null) {
            throw new IllegalArgumentException("chunk must not be null");
        }
    }
}
