package com.xiaoding.javaai.knowledge.retrieval.web;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalResult;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;

import java.util.List;

record KnowledgeRetrievalEvaluationResponse(
        String embeddingModel,
        List<String> chunkIds,
        List<String> documentIds
) {
    static KnowledgeRetrievalEvaluationResponse from(KnowledgeRetrievalResult result) {
        return new KnowledgeRetrievalEvaluationResponse(
                result.embeddingModel(),
                result.chunks().stream().map(RetrievedKnowledgeChunk::chunkId).toList(),
                result.chunks().stream().map(chunk -> chunk.documentId().value()).toList()
        );
    }

    KnowledgeRetrievalEvaluationResponse {
        chunkIds = List.copyOf(chunkIds);
        documentIds = List.copyOf(documentIds);
    }
}
