package com.xiaoding.javaai.knowledge.retrieval.application.port;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;

import java.time.Instant;
import java.util.List;

public interface KnowledgeLexicalSearchRepository {
    List<RetrievedKnowledgeChunk> search(
            String query,
            KnowledgeAccessScope scope,
            Instant effectiveAt,
            int topK
    );
}
