package com.xiaoding.javaai.knowledge.retrieval.application.port;

import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;

import java.util.List;

public interface KnowledgeReranker {
    List<String> rerank(
            String question,
            List<RetrievedKnowledgeChunk> candidates,
            int topK
    );
}
