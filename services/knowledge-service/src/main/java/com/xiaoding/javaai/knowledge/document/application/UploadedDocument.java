package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.ContentHash;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

public record UploadedDocument(
        DocumentId documentId,
        TenantId tenantId,
        int versionNumber,
        long revision,
        ContentHash contentHash,
        ObjectKey objectKey
) {
}
