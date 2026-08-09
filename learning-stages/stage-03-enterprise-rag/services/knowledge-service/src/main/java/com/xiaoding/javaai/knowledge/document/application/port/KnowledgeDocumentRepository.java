package com.xiaoding.javaai.knowledge.document.application.port;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.KnowledgeDocument;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.util.Optional;

public interface KnowledgeDocumentRepository {
    Optional<KnowledgeDocument> find(TenantId tenantId, DocumentId documentId);

    void save(KnowledgeDocument document);
}
