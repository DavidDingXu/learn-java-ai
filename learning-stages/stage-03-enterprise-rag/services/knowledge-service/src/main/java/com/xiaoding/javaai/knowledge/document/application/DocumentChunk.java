package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.util.List;

public record DocumentChunk(
        String chunkId,
        TenantId tenantId,
        DocumentId documentId,
        int documentVersion,
        String chunkPolicyVersion,
        int ordinal,
        List<String> headingPath,
        String clause,
        String text
) {
    public DocumentChunk {
        headingPath = List.copyOf(headingPath);
    }
}
