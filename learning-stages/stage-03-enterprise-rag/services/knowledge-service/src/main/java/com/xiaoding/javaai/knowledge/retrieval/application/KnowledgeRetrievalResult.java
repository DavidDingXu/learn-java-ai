package com.xiaoding.javaai.knowledge.retrieval.application;

import java.util.List;

public record KnowledgeRetrievalResult(
        String embeddingModel,
        List<RetrievedKnowledgeChunk> chunks
) {
    public KnowledgeRetrievalResult {
        chunks = List.copyOf(chunks);
    }
}
