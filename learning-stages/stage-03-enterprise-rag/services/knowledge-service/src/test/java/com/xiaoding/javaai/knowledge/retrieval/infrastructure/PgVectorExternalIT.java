package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.application.DocumentChunk;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import com.xiaoding.javaai.knowledge.indexing.infrastructure.PgVectorKnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorExternalIT {

    @Test
    void migrates_pgvector_and_filters_acl_before_topk() throws Exception {
        PGSimpleDataSource dataSource = migratedDataSource();

        seed(dataSource);
        PgVectorKnowledgeChunkSearchRepository repository =
                new PgVectorKnowledgeChunkSearchRepository(dataSource, new ObjectMapper());

        var result = repository.search(
                embedding(1.0f, 0.0f),
                "deterministic-test",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of("support")),
                Instant.parse("2026-07-12T10:00:00Z"),
                1
        );

        assertThat(result).extracting(chunk -> chunk.chunkId()).containsExactly("allowed-chunk");
    }

    @Test
    void writes_a_document_version_with_the_index_sink_and_reads_it_through_acl_search() throws Exception {
        PGSimpleDataSource dataSource = migratedDataSource();
        Instant now = Instant.parse("2026-07-12T10:00:00Z");
        seedMetadata(dataSource, now);
        PgVectorKnowledgeChunkIndexSink sink = new PgVectorKnowledgeChunkIndexSink(
                dataSource, new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC));
        float[] vector = embedding(1.0f, 0.0f);

        sink.replaceVersion(
                new ClaimedIndexTask(
                        taskId(1),
                        new TenantId("tenant-a"),
                        new DocumentId("refund-policy"),
                        1,
                        IndexTaskType.REINDEX_DOCUMENT_VERSION,
                        1
                ),
                List.of(new DocumentChunk(
                        "sink-written-chunk",
                        new TenantId("tenant-a"),
                        new DocumentId("refund-policy"),
                        1,
                        "policy-chunk-v1",
                        1,
                        List.of("退款政策"),
                        "第十条",
                        "退款审核通过后原路退回。"
                )),
                List.of(new KnowledgeEmbedding(vector, "deterministic-test"))
        );

        var result = new PgVectorKnowledgeChunkSearchRepository(dataSource, new ObjectMapper()).search(
                vector,
                "deterministic-test",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of()),
                now,
                1
        );

        assertThat(result).singleElement().satisfies(chunk -> {
            assertThat(chunk.chunkId()).isEqualTo("sink-written-chunk");
            assertThat(chunk.headingPath()).containsExactly("退款政策");
            assertThat(chunk.clause()).isEqualTo("第十条");
        });
    }

    @Test
    void keeps_serving_the_previous_index_until_the_replacement_is_ready() throws Exception {
        PGSimpleDataSource dataSource = migratedDataSource();
        Instant now = Instant.parse("2026-07-12T10:00:00Z");
        seedMetadata(dataSource, now);
        PgVectorKnowledgeChunkIndexSink sink = new PgVectorKnowledgeChunkIndexSink(
                dataSource, new ObjectMapper(), Clock.fixed(now, ZoneOffset.UTC));
        float[] vector = embedding(1.0f, 0.0f);
        writeVersion(sink, 1, "refund-v1", "旧版退款时效", vector);
        publishReplacement(dataSource, now);

        PgVectorKnowledgeChunkSearchRepository repository =
                new PgVectorKnowledgeChunkSearchRepository(dataSource, new ObjectMapper());
        var whileIndexing = repository.search(
                vector,
                "deterministic-test",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of()),
                now,
                1
        );

        assertThat(whileIndexing).singleElement().satisfies(chunk -> {
            assertThat(chunk.documentVersion()).isEqualTo(1);
            assertThat(chunk.content()).isEqualTo("旧版退款时效");
        });

        writeVersion(sink, 2, "refund-v2", "新版退款时效", vector);

        var afterIndexing = repository.search(
                vector,
                "deterministic-test",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of()),
                now,
                1
        );
        assertThat(afterIndexing).singleElement().satisfies(chunk -> {
            assertThat(chunk.documentVersion()).isEqualTo(2);
            assertThat(chunk.content()).isEqualTo("新版退款时效");
        });
    }

    private static PGSimpleDataSource migratedDataSource() {
        PGSimpleDataSource dataSource = dataSourceFromEnvironment();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(4);
        return dataSource;
    }

    private static PGSimpleDataSource dataSourceFromEnvironment() {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(required("JAVA_AI_POSTGRES_URL"));
        dataSource.setUser(required("JAVA_AI_POSTGRES_USER"));
        dataSource.setPassword(required("JAVA_AI_POSTGRES_PASSWORD"));
        return dataSource;
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " must be configured");
        return value;
    }

    private static void seed(PGSimpleDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            insertDocument(connection, "tenant-a", "allowed", "allowed-chunk", "USER", "user-1",
                    embedding(0.8f, 0.6f));
            insertDocument(connection, "tenant-a", "forbidden", "forbidden-chunk", "DEPARTMENT", "finance",
                    embedding(1.0f, 0.0f));
            insertDocument(connection, "tenant-b", "other-tenant", "other-tenant-chunk", "USER", "user-1",
                    embedding(0.9f, 0.4358899f));
            connection.commit();
        }
    }

    private static void seedMetadata(PGSimpleDataSource dataSource, Instant now) throws Exception {
        OffsetDateTime databaseTimestamp = now.atOffset(ZoneOffset.UTC);
        OffsetDateTime existingVersionTimestamp = now.minusSeconds(3600).atOffset(ZoneOffset.UTC);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO knowledge_document
                        (tenant_id, document_id, title, revision, created_by, created_at, updated_at)
                    VALUES ('tenant-a', 'refund-policy', '退款政策', 2, 'editor-1', ?, ?)
                    """)) {
                statement.setObject(1, existingVersionTimestamp);
                statement.setObject(2, databaseTimestamp);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO document_version
                        (tenant_id, document_id, version_number, content_hash, object_key, media_type,
                         status, effective_from, created_by, created_at)
                    VALUES (
                        'tenant-a', 'refund-policy', 1, 'sha256:refund-policy',
                        'tenant-a/knowledge/refund-policy/source.md', 'text/markdown',
                        'PUBLISHED', ?, 'editor-1', ?
                    )
                    """)) {
                statement.setObject(1, existingVersionTimestamp);
                statement.setObject(2, existingVersionTimestamp);
                statement.executeUpdate();
            }
            insertRunningIndexTask(connection, 1, now);
            try (var statement = connection.prepareStatement("""
                    INSERT INTO document_acl
                        (tenant_id, document_id, subject_type, subject_id, permission, created_at)
                    VALUES ('tenant-a', 'refund-policy', 'USER', 'user-1', 'READ', ?)
                    """)) {
                statement.setObject(1, databaseTimestamp);
                statement.executeUpdate();
            }
            connection.commit();
        }
    }

    private static void publishReplacement(PGSimpleDataSource dataSource, Instant now) throws Exception {
        OffsetDateTime databaseTimestamp = now.atOffset(ZoneOffset.UTC);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.prepareStatement("""
                    UPDATE document_version
                       SET status = 'RETIRED', effective_until = ?
                     WHERE tenant_id = 'tenant-a'
                       AND document_id = 'refund-policy'
                       AND version_number = 1
                    """)) {
                statement.setObject(1, databaseTimestamp);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("""
                    INSERT INTO document_version
                        (tenant_id, document_id, version_number, content_hash, object_key, media_type,
                         status, effective_from, created_by, created_at)
                    VALUES (
                        'tenant-a', 'refund-policy', 2, 'sha256:refund-policy-v2',
                        'tenant-a/knowledge/refund-policy/source-v2.md', 'text/markdown',
                        'PUBLISHED', ?, 'editor-1', ?
                    )
                    """)) {
                statement.setObject(1, databaseTimestamp);
                statement.setObject(2, databaseTimestamp);
                statement.executeUpdate();
            }
            insertRunningIndexTask(connection, 2, now);
            connection.commit();
        }
    }

    private static void writeVersion(
            PgVectorKnowledgeChunkIndexSink sink,
            int version,
            String chunkId,
            String content,
            float[] vector
    ) {
        sink.replaceVersion(
                new ClaimedIndexTask(
                        taskId(version),
                        new TenantId("tenant-a"),
                        new DocumentId("refund-policy"),
                        version,
                        IndexTaskType.REINDEX_DOCUMENT_VERSION,
                        1
                ),
                List.of(new DocumentChunk(
                        chunkId,
                        new TenantId("tenant-a"),
                        new DocumentId("refund-policy"),
                        version,
                        "policy-chunk-v1",
                        1,
                        List.of("退款政策"),
                        "第十条",
                        content
                )),
                List.of(new KnowledgeEmbedding(vector, "deterministic-test"))
        );
    }

    private static UUID taskId(int version) {
        return UUID.nameUUIDFromBytes(
                ("refund-policy-v" + version).getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    private static void insertRunningIndexTask(Connection connection, int version, Instant now) throws Exception {
        OffsetDateTime databaseTimestamp = now.atOffset(ZoneOffset.UTC);
        try (var statement = connection.prepareStatement("""
                INSERT INTO index_task (
                    task_id, tenant_id, document_id, version_number, task_type, status,
                    attempts, lease_owner, lease_until, next_attempt_at, created_at, updated_at
                ) VALUES (?, 'tenant-a', 'refund-policy', ?, 'REINDEX_DOCUMENT_VERSION', 'RUNNING',
                          1, 'external-it', ?, ?, ?, ?)
                """)) {
            statement.setObject(1, taskId(version));
            statement.setInt(2, version);
            statement.setObject(3, now.plusSeconds(300).atOffset(ZoneOffset.UTC));
            statement.setObject(4, databaseTimestamp);
            statement.setObject(5, databaseTimestamp);
            statement.setObject(6, databaseTimestamp);
            statement.executeUpdate();
        }
    }

    private static void insertDocument(
            Connection connection,
            String tenantId,
            String documentId,
            String chunkId,
            String subjectType,
            String subjectId,
            float[] embedding
    ) throws Exception {
        Instant now = Instant.parse("2026-07-12T09:00:00Z");
        OffsetDateTime databaseTimestamp = now.atOffset(ZoneOffset.UTC);
        try (var statement = connection.prepareStatement("""
                INSERT INTO knowledge_document
                    (tenant_id, document_id, title, revision, created_by, created_at, updated_at)
                VALUES (?, ?, ?, 1, 'test', ?, ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, documentId);
            statement.setString(3, documentId);
            statement.setObject(4, databaseTimestamp);
            statement.setObject(5, databaseTimestamp);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO document_version
                    (tenant_id, document_id, version_number, content_hash, object_key, media_type,
                     status, effective_from, created_by, created_at)
                VALUES (?, ?, 1, ?, ?, 'text/plain', 'PUBLISHED', ?, 'test', ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, documentId);
            statement.setString(3, "sha256:" + documentId);
            statement.setString(4, tenantId + "/" + documentId + ".txt");
            statement.setObject(5, databaseTimestamp);
            statement.setObject(6, databaseTimestamp);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO document_chunk
                    (tenant_id, document_id, version_number, chunk_id, chunk_policy_version,
                     ordinal, content, embedding, embedding_model, created_at)
                VALUES (?, ?, 1, ?, 'test-v1', 1, ?, ?::vector, 'deterministic-test', ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, documentId);
            statement.setString(3, chunkId);
            statement.setString(4, "content " + documentId);
            statement.setString(5, vectorLiteral(embedding));
            statement.setObject(6, databaseTimestamp);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO document_search_version
                    (tenant_id, document_id, version_number, activated_at)
                VALUES (?, ?, 1, ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, documentId);
            statement.setObject(3, databaseTimestamp);
            statement.executeUpdate();
        }
        try (var statement = connection.prepareStatement("""
                INSERT INTO document_acl
                    (tenant_id, document_id, subject_type, subject_id, permission, created_at)
                VALUES (?, ?, ?, ?, 'READ', ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, documentId);
            statement.setString(3, subjectType);
            statement.setString(4, subjectId);
            statement.setObject(5, databaseTimestamp);
            statement.executeUpdate();
        }
    }

    private static float[] embedding(float first, float second) {
        float[] vector = new float[1536];
        vector[0] = first;
        vector[1] = second;
        return vector;
    }

    private static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int index = 0; index < vector.length; index += 1) {
            if (index > 0) value.append(',');
            value.append(vector[index]);
        }
        return value.append(']').toString();
    }
}
