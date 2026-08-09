package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.ParsedDocument;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentObjectStore;
import com.xiaoding.javaai.knowledge.document.application.port.KnowledgeDocumentRepository;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.ContentHash;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.KnowledgeDocument;
import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import com.xiaoding.javaai.knowledge.indexing.application.IndexingException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoredDocumentVersionTextSourceTest {

    @Test
    void rejects_a_late_task_for_a_retired_version_before_reading_its_object() {
        Instant now = Instant.parse("2026-07-17T02:00:00Z");
        TenantId tenantId = new TenantId("tenant-a");
        DocumentId documentId = new DocumentId("refund-policy");
        ActorId actorId = new ActorId("editor-1");
        KnowledgeDocument document = KnowledgeDocument.create(documentId, tenantId, "退款政策", actorId, now);
        document.addVersion(0, new ContentHash("sha256:v1"), new ObjectKey("v1.md"),
                "text/markdown", actorId, now);
        document.publishVersion(1, 1, now, null);
        document.addVersion(2, new ContentHash("sha256:v2"), new ObjectKey("v2.md"),
                "text/markdown", actorId, now.plusSeconds(1));
        document.publishVersion(3, 2, now.plusSeconds(1), null);
        KnowledgeDocumentRepository repository = new KnowledgeDocumentRepository() {
            @Override
            public Optional<KnowledgeDocument> find(TenantId ignoredTenant, DocumentId ignoredDocument) {
                return Optional.of(document);
            }

            @Override
            public void save(KnowledgeDocument ignored) {
            }
        };
        DocumentObjectStore objectStore = new DocumentObjectStore() {
            @Override
            public void put(ObjectKey key, String mediaType, byte[] content) {
            }

            @Override
            public byte[] get(ObjectKey key) {
                throw new AssertionError("retired source must not be read");
            }
        };
        StoredDocumentVersionTextSource source = new StoredDocumentVersionTextSource(
                repository, objectStore, (mediaType, content) -> new ParsedDocument("ignored"));

        assertThatThrownBy(() -> source.loadText(new ClaimedIndexTask(
                UUID.randomUUID(), tenantId, documentId, 1,
                IndexTaskType.REINDEX_DOCUMENT_VERSION, 1
        )))
                .isInstanceOf(IndexingException.class)
                .extracting(error -> ((IndexingException) error).errorCode())
                .isEqualTo("DOCUMENT_VERSION_NOT_PUBLISHED");
    }
}
