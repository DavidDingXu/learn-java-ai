package com.xiaoding.javaai.knowledge.indexing.application.port;

import com.xiaoding.javaai.knowledge.document.application.DocumentChunk;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;

import java.util.List;

public interface KnowledgeChunkIndexSink {
    void replaceVersion(
            ClaimedIndexTask task,
            List<DocumentChunk> chunks,
            List<KnowledgeEmbedding> embeddings
    );
}
