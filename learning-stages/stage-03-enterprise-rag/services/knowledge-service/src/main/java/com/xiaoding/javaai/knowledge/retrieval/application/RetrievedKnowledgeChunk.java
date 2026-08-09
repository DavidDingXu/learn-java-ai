package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;

import java.util.List;

public record RetrievedKnowledgeChunk(
        String chunkId,
        DocumentId documentId,
        int documentVersion,
        List<String> headingPath,
        String clause,
        String content,
        double score
) {
    public RetrievedKnowledgeChunk {
        headingPath = List.copyOf(headingPath);
    }
}
