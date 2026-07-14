package com.xiaoding.javaai.knowledge.retrieval.application.port;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;

import java.util.List;

public interface KnowledgeEmbeddingModel {
    KnowledgeEmbedding embed(String text);

    default List<KnowledgeEmbedding> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("texts must not be empty");
        }
        return texts.stream().map(this::embed).toList();
    }
}
