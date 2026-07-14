package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeChunkSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;

public final class KnowledgeRetrievalService implements KnowledgeRetriever {

    private final KnowledgeEmbeddingModel embeddingModel;
    private final KnowledgeChunkSearchRepository searchRepository;

    public KnowledgeRetrievalService(
            KnowledgeEmbeddingModel embeddingModel,
            KnowledgeChunkSearchRepository searchRepository
    ) {
        this.embeddingModel = embeddingModel;
        this.searchRepository = searchRepository;
    }

    @Override
    public KnowledgeRetrievalResult retrieve(RetrieveKnowledgeQuery query) {
        KnowledgeEmbedding embedding = embeddingModel.embed(query.question());
        var chunks = searchRepository.search(
                embedding.vector(), embedding.model(), query.accessScope(), query.effectiveAt(), query.topK()
        );
        return new KnowledgeRetrievalResult(embedding.model(), chunks);
    }
}
