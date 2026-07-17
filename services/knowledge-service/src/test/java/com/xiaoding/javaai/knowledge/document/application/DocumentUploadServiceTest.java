package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.application.port.DocumentContentParser;
import com.xiaoding.javaai.knowledge.document.application.port.DocumentObjectStore;
import com.xiaoding.javaai.knowledge.document.application.port.KnowledgeDocumentRepository;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.KnowledgeDocument;
import com.xiaoding.javaai.knowledge.document.domain.ObjectKey;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-12T09:00:00Z");

    @Test
    void stores_a_parsed_document_under_a_tenant_isolated_key() {
        InMemoryRepository repository = new InMemoryRepository();
        RecordingObjectStore objectStore = new RecordingObjectStore();
        DocumentUploadService service = new DocumentUploadService(
                repository, objectStore, (mediaType, content) -> new ParsedDocument("# 退款政策\n到账时间")
        );

        UploadedDocument uploaded = service.upload(command("text/markdown", "# 退款政策".getBytes(StandardCharsets.UTF_8)));

        assertThat(uploaded.versionNumber()).isEqualTo(1);
        assertThat(uploaded.contentHash().value()).startsWith("sha256:");
        assertThat(uploaded.objectKey().value()).startsWith("tenant-a/knowledge/refund-policy/");
        assertThat(objectStore.objects).containsKey(uploaded.objectKey());
        KnowledgeDocument stored = repository.find(
                new TenantId("tenant-a"), new DocumentId("refund-policy")
        ).orElseThrow();
        assertThat(stored.version(1).objectKey()).isEqualTo(uploaded.objectKey());
    }

    @Test
    void rejects_an_unsupported_media_type_before_parsing_or_storing() {
        RecordingParser parser = new RecordingParser();
        RecordingObjectStore objectStore = new RecordingObjectStore();
        DocumentUploadService service = new DocumentUploadService(
                new InMemoryRepository(), objectStore, parser
        );

        assertThatThrownBy(() -> service.upload(command("application/x-msdownload", new byte[]{1, 2, 3})))
                .isInstanceOf(UnsupportedDocumentMediaTypeException.class);

        assertThat(parser.calls).isZero();
        assertThat(objectStore.objects).isEmpty();
    }

    @Test
    void rejects_a_document_larger_than_the_upload_limit() {
        RecordingObjectStore objectStore = new RecordingObjectStore();
        DocumentUploadService service = new DocumentUploadService(
                new InMemoryRepository(), objectStore, (mediaType, content) -> new ParsedDocument("ignored")
        );

        assertThatThrownBy(() -> service.upload(command("text/plain", new byte[5 * 1024 * 1024 + 1])))
                .isInstanceOf(DocumentTooLargeException.class);

        assertThat(objectStore.objects).isEmpty();
    }

    @Test
    void leaves_no_object_or_document_when_parsing_fails() {
        InMemoryRepository repository = new InMemoryRepository();
        RecordingObjectStore objectStore = new RecordingObjectStore();
        DocumentUploadService service = new DocumentUploadService(
                repository, objectStore, (mediaType, content) -> {
                    throw new DocumentParsingException("invalid markdown encoding");
                }
        );

        assertThatThrownBy(() -> service.upload(command("text/markdown", new byte[]{(byte) 0xff})))
                .isInstanceOf(DocumentParsingException.class);

        assertThat(repository.documents).isEmpty();
        assertThat(objectStore.objects).isEmpty();
    }

    @Test
    void rejects_duplicate_content_without_writing_a_second_object() {
        InMemoryRepository repository = new InMemoryRepository();
        RecordingObjectStore objectStore = new RecordingObjectStore();
        DocumentUploadService service = new DocumentUploadService(
                repository, objectStore, (mediaType, content) -> new ParsedDocument("same")
        );
        byte[] content = "same".getBytes(StandardCharsets.UTF_8);
        service.upload(command("text/plain", content));

        assertThatThrownBy(() -> service.upload(new UploadDocumentCommand(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                "refund-v2.txt",
                "text/plain",
                content,
                1,
                NOW.plusSeconds(60)
        ))).isInstanceOf(com.xiaoding.javaai.knowledge.document.domain.DuplicateDocumentContentException.class);

        assertThat(objectStore.objects).hasSize(1);
    }

    private static UploadDocumentCommand command(String mediaType, byte[] content) {
        return new UploadDocumentCommand(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                "refund.md",
                mediaType,
                content,
                0,
                NOW
        );
    }

    private static final class InMemoryRepository implements KnowledgeDocumentRepository {
        private final Map<String, KnowledgeDocument> documents = new HashMap<>();

        @Override
        public Optional<KnowledgeDocument> find(TenantId tenantId, DocumentId documentId) {
            return Optional.ofNullable(documents.get(key(tenantId, documentId)));
        }

        @Override
        public void save(KnowledgeDocument document) {
            documents.put(key(document.tenantId(), document.id()), document);
        }

        private static String key(TenantId tenantId, DocumentId documentId) {
            return tenantId.value() + ":" + documentId.value();
        }
    }

    private static final class RecordingObjectStore implements DocumentObjectStore {
        private final Map<ObjectKey, byte[]> objects = new HashMap<>();

        @Override
        public void put(ObjectKey key, String mediaType, byte[] content) {
            objects.put(key, content.clone());
        }

        @Override
        public byte[] get(ObjectKey key) {
            byte[] content = objects.get(key);
            return content == null ? null : content.clone();
        }
    }

    private static final class RecordingParser implements DocumentContentParser {
        private int calls;

        @Override
        public ParsedDocument parse(String mediaType, byte[] content) {
            calls += 1;
            return new ParsedDocument("parsed");
        }
    }
}
