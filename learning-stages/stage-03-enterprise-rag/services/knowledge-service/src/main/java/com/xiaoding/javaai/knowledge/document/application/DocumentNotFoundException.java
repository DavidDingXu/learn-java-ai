package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

public final class DocumentNotFoundException extends RuntimeException {
    public DocumentNotFoundException(TenantId tenantId, DocumentId documentId) {
        super("document " + documentId.value() + " does not exist in tenant " + tenantId.value());
    }
}
