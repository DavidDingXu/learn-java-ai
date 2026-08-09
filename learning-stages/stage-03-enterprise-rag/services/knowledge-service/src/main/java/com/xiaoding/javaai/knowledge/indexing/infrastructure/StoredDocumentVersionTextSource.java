package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.DocumentNotFoundException;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentContentParser;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentObjectStore;
import com.xiaoding.javaai.knowledge.document.application.port.KnowledgeDocumentRepository;
import com.xiaoding.javaai.knowledge.document.domain.DocumentVersionStatus;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.IndexingException;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionTextSource;

public final class StoredDocumentVersionTextSource implements DocumentVersionTextSource {

    private final KnowledgeDocumentRepository repository;
    private final DocumentObjectStore objectStore;
    private final DocumentContentParser parser;

    public StoredDocumentVersionTextSource(
            KnowledgeDocumentRepository repository,
            DocumentObjectStore objectStore,
            DocumentContentParser parser
    ) {
        this.repository = repository;
        this.objectStore = objectStore;
        this.parser = parser;
    }

    @Override
    public String loadText(ClaimedIndexTask task) {
        var document = repository.find(task.tenantId(), task.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(task.tenantId(), task.documentId()));
        var version = document.version(task.documentVersion());
        if (version.status() != DocumentVersionStatus.PUBLISHED) {
            throw new IndexingException(
                    "DOCUMENT_VERSION_NOT_PUBLISHED",
                    "only the currently published document version can be indexed"
            );
        }
        return parser.parse(version.mediaType(), objectStore.get(version.objectKey())).text();
    }
}
