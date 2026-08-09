package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.time.Instant;

public record UploadDocumentCommand(
        DocumentId documentId,
        TenantId tenantId,
        String title,
        ActorId actorId,
        String fileName,
        String mediaType,
        byte[] content,
        long expectedRevision,
        Instant uploadedAt
) {
    public UploadDocumentCommand {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        if (fileName == null || fileName.isBlank()) throw new IllegalArgumentException("fileName must not be blank");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType must not be blank");
        if (content == null) throw new IllegalArgumentException("content must not be null");
        if (uploadedAt == null) throw new IllegalArgumentException("uploadedAt must not be null");
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
