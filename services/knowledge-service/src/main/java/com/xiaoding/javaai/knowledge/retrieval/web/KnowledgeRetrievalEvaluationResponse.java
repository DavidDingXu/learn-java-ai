package com.xiaoding.javaai.knowledge.retrieval.web;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalResult;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;

import java.util.List;

record KnowledgeRetrievalEvaluationResponse(
        String embeddingModel,
        List<String> chunkIds
) {
    static KnowledgeRetrievalEvaluationResponse from(KnowledgeRetrievalResult result) {
        return new KnowledgeRetrievalEvaluationResponse(
                result.embeddingModel(),
                result.chunks().stream().map(RetrievedKnowledgeChunk::chunkId).toList()
        );
    }

    KnowledgeRetrievalEvaluationResponse {
        chunkIds = List.copyOf(chunkIds);
    }
}
