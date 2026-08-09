package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.util.UUID;

public record PublishedDocument(
        TenantId tenantId,
        DocumentId documentId,
        int versionNumber,
        long revision,
        UUID indexTaskId
) {
}
