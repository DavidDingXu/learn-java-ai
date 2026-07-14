package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

public record ChunkDocumentCommand(
        TenantId tenantId,
        DocumentId documentId,
        int documentVersion,
        String chunkPolicyVersion,
        String text
) {
    public ChunkDocumentCommand {
        if (documentVersion < 1) throw new IllegalArgumentException("documentVersion must be positive");
        if (chunkPolicyVersion == null || chunkPolicyVersion.isBlank()) {
            throw new IllegalArgumentException("chunkPolicyVersion must not be blank");
        }
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text must not be blank");
    }
}
