package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PgVectorExternalIT {

    @Test
    void migrates_pgvector_and_filters_acl_before_topk() throws Exception {
        PGSimpleDataSource dataSource = dataSourceFromEnvironment();
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(2);

        seed(dataSource);
        PgVectorKnowledgeChunkSearchRepository repository =
                new PgVectorKnowledgeChunkSearchRepository(dataSource, new ObjectMapper());
        float[] embedding = embedding();

        var result = repository.search(
                embedding,
                "deterministic-test",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of("support")),
                Instant.parse("2026-07-12T10:00:00Z"),
                1
        );

        assertThat(result).extracting(chunk -> chunk.chunkId()).containsExactly("allowed-chunk");
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
            insertDocument(connection, "tenant-a", "allowed", "allowed-chunk", "USER", "user-1");
            insertDocument(connection, "tenant-a", "forbidden", "forbidden-chunk", "DEPARTMENT", "finance");
            insertDocument(connection, "tenant-b", "other-tenant", "other-tenant-chunk", "USER", "user-1");
            connection.commit();
        }
    }

    private static void insertDocument(
            Connection connection,
            String tenantId,
            String documentId,
            String chunkId,
            String subjectType,
            String subjectId
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
            statement.setString(5, vectorLiteral(embedding()));
            statement.setObject(6, databaseTimestamp);
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

    private static float[] embedding() {
        float[] vector = new float[1536];
        vector[0] = 1.0f;
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
