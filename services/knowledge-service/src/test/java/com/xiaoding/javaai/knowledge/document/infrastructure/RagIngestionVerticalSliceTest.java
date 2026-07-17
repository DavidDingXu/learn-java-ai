package com.xiaoding.javaai.knowledge.document.infrastructure;

import com.xiaoding.javaai.knowledge.document.application.DocumentAclGrant;
import com.xiaoding.javaai.knowledge.document.application.DocumentAclSubjectType;
import com.xiaoding.javaai.knowledge.document.application.DocumentPublicationService;
import com.xiaoding.javaai.knowledge.document.application.DocumentUploadService;
import com.xiaoding.javaai.knowledge.document.application.PolicyDocumentChunker;
import com.xiaoding.javaai.knowledge.document.application.PublishDocumentCommand;
import com.xiaoding.javaai.knowledge.document.application.UploadDocumentCommand;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.DocumentVersionStatus;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.DocumentVersionIndexingService;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import com.xiaoding.javaai.knowledge.indexing.application.port.KnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.indexing.infrastructure.StoredDocumentVersionTextSource;
import com.xiaoding.javaai.knowledge.retrieval.infrastructure.DeterministicHashEmbeddingModel;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RagIngestionVerticalSliceTest {

    private static final Instant NOW = Instant.parse("2026-07-17T02:00:00Z");

    @TempDir
    Path objectRoot;

    @Test
    void uploads_publishes_and_indexes_a_tenant_scoped_document() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:rag-ingestion;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        createSchema(jdbc);
        JdbcKnowledgeDocumentRepository repository = new JdbcKnowledgeDocumentRepository(
                jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(dataSource))
        );
        LocalFileDocumentObjectStore objectStore = new LocalFileDocumentObjectStore(objectRoot);
        Utf8TextDocumentContentParser parser = new Utf8TextDocumentContentParser();
        DocumentUploadService uploadService = new DocumentUploadService(repository, objectStore, parser);
        DocumentPublicationService publicationService = new DocumentPublicationService(
                repository,
                repository,
                () -> UUID.fromString("00000000-0000-0000-0000-000000000099"),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );

        var uploaded = uploadService.upload(new UploadDocumentCommand(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                "refund.md",
                "text/markdown",
                "# 退款政策\n\n第十条 审核通过后五个工作日内到账。".getBytes(StandardCharsets.UTF_8),
                0,
                NOW.minusSeconds(60)
        ));
        var published = publicationService.publish(new PublishDocumentCommand(
                uploaded.tenantId(),
                uploaded.documentId(),
                new ActorId("publisher-1"),
                uploaded.versionNumber(),
                1,
                NOW,
                null,
                List.of(new DocumentAclGrant(DocumentAclSubjectType.DEPARTMENT, "support"))
        ));

        assertThat(repository.find(uploaded.tenantId(), uploaded.documentId()).orElseThrow()
                .version(1).status()).isEqualTo(DocumentVersionStatus.PUBLISHED);
        assertThat(jdbc.queryForObject("select count(*) from document_acl", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select status from index_task", String.class)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select published_by from document_version", String.class))
                .isEqualTo("publisher-1");
        assertThat(published.indexTaskId()).isEqualTo(
                UUID.fromString("00000000-0000-0000-0000-000000000099"));

        RecordingSink sink = new RecordingSink();
        var indexing = new DocumentVersionIndexingService(
                new StoredDocumentVersionTextSource(repository, objectStore, parser),
                new PolicyDocumentChunker(200),
                new DeterministicHashEmbeddingModel(16),
                sink,
                "policy-chunk-v1",
                8
        );
        indexing.index(new ClaimedIndexTask(
                published.indexTaskId(),
                uploaded.tenantId(),
                uploaded.documentId(),
                uploaded.versionNumber(),
                IndexTaskType.REINDEX_DOCUMENT_VERSION,
                1
        ));

        assertThat(sink.chunks).singleElement().satisfies(chunk -> {
            assertThat(chunk.headingPath()).containsExactly("退款政策");
            assertThat(chunk.text()).contains("五个工作日");
        });
        assertThat(sink.embeddings).singleElement().satisfies(embedding ->
                assertThat(embedding.vector()).hasSize(16));
    }

    private static void createSchema(JdbcTemplate jdbc) {
        jdbc.execute("""
                create table knowledge_document (
                    tenant_id varchar(128) not null, document_id varchar(160) not null,
                    title varchar(500) not null, status varchar(32) not null default 'ACTIVE',
                    revision bigint not null, created_by varchar(160) not null,
                    created_at timestamp with time zone not null,
                    updated_at timestamp with time zone not null,
                    primary key (tenant_id, document_id)
                )
                """);
        jdbc.execute("""
                create table document_version (
                    tenant_id varchar(128) not null, document_id varchar(160) not null,
                    version_number integer not null, content_hash varchar(80) not null,
                    object_key varchar(900) not null, media_type varchar(160) not null,
                    status varchar(32) not null, effective_from timestamp with time zone,
                    effective_until timestamp with time zone, published_by varchar(160),
                    published_at timestamp with time zone, created_by varchar(160) not null,
                    created_at timestamp with time zone not null,
                    primary key (tenant_id, document_id, version_number)
                )
                """);
        jdbc.execute("""
                create table document_acl (
                    tenant_id varchar(128) not null, document_id varchar(160) not null,
                    subject_type varchar(32) not null, subject_id varchar(160) not null,
                    permission varchar(32) not null, created_at timestamp with time zone not null,
                    primary key (tenant_id, document_id, subject_type, subject_id, permission)
                )
                """);
        jdbc.execute("""
                create table index_task (
                    task_id uuid primary key, tenant_id varchar(128) not null,
                    document_id varchar(160) not null, version_number integer not null,
                    task_type varchar(32) not null, status varchar(32) not null,
                    attempts integer not null, lease_owner varchar(160),
                    lease_until timestamp with time zone, next_attempt_at timestamp with time zone not null,
                    error_code varchar(80), error_message varchar(1000),
                    created_at timestamp with time zone not null, updated_at timestamp with time zone not null,
                    unique (tenant_id, document_id, version_number, task_type)
                )
                """);
    }

    private static final class RecordingSink implements KnowledgeChunkIndexSink {
        private List<com.xiaoding.javaai.knowledge.document.application.DocumentChunk> chunks;
        private List<com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding> embeddings;

        @Override
        public void replaceVersion(
                ClaimedIndexTask task,
                List<com.xiaoding.javaai.knowledge.document.application.DocumentChunk> chunks,
                List<com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding> embeddings
        ) {
            this.chunks = chunks;
            this.embeddings = embeddings;
        }
    }
}
