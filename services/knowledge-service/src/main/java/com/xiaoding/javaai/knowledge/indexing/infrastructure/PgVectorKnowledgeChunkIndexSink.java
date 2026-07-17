package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.application.DocumentChunk;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.IndexingException;
import com.xiaoding.javaai.knowledge.indexing.application.port.KnowledgeChunkIndexSink;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.StringJoiner;

public final class PgVectorKnowledgeChunkIndexSink implements KnowledgeChunkIndexSink {

    private static final String FENCE_SQL = """
            select 1
              from index_task
             where task_id = ?
               and tenant_id = ?
               and document_id = ?
               and version_number = ?
               and task_type = ?
               and status = 'RUNNING'
               and attempts = ?
               and lease_until > ?
             for update
            """;
    private static final String DELETE_SQL = """
            delete from document_chunk
             where tenant_id = ? and document_id = ? and version_number = ?
            """;
    private static final String INSERT_SQL = """
            insert into document_chunk (
                tenant_id, document_id, version_number, chunk_id, chunk_policy_version,
                ordinal, heading_path, clause, content, embedding, embedding_model, created_at
            ) values (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?::vector, ?, ?)
            """;
    private static final String ACTIVATE_SQL = """
            insert into document_search_version (tenant_id, document_id, version_number, activated_at)
            select tenant_id, document_id, version_number, ?
              from document_version
             where tenant_id = ?
               and document_id = ?
               and version_number = ?
               and status = 'PUBLISHED'
               and effective_from <= ?
               and (effective_until is null or effective_until > ?)
            on conflict (tenant_id, document_id) do update
               set version_number = excluded.version_number,
                   activated_at = excluded.activated_at
            """;

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PgVectorKnowledgeChunkIndexSink(DataSource dataSource, ObjectMapper objectMapper, Clock clock) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public void replaceVersion(
            ClaimedIndexTask task,
            List<DocumentChunk> chunks,
            List<KnowledgeEmbedding> embeddings
    ) {
        validate(task, chunks, embeddings);
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
            OffsetDateTime transactionTime = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
            assertCurrentLease(connection, task, transactionTime);
            try (var delete = connection.prepareStatement(DELETE_SQL)) {
                delete.setString(1, task.tenantId().value());
                delete.setString(2, task.documentId().value());
                delete.setInt(3, task.documentVersion());
                delete.executeUpdate();
            }
            try (var insert = connection.prepareStatement(INSERT_SQL)) {
                for (int index = 0; index < chunks.size(); index += 1) {
                    DocumentChunk chunk = chunks.get(index);
                    KnowledgeEmbedding embedding = embeddings.get(index);
                    insert.setString(1, task.tenantId().value());
                    insert.setString(2, task.documentId().value());
                    insert.setInt(3, task.documentVersion());
                    insert.setString(4, chunk.chunkId());
                    insert.setString(5, chunk.chunkPolicyVersion());
                    insert.setInt(6, chunk.ordinal());
                    insert.setString(7, json(chunk.headingPath()));
                    insert.setString(8, chunk.clause());
                    insert.setString(9, chunk.text());
                    insert.setString(10, vectorLiteral(embedding.vector()));
                    insert.setString(11, embedding.model());
                    insert.setObject(12, transactionTime);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            try (var activate = connection.prepareStatement(ACTIVATE_SQL)) {
                activate.setObject(1, transactionTime);
                activate.setString(2, task.tenantId().value());
                activate.setString(3, task.documentId().value());
                activate.setInt(4, task.documentVersion());
                activate.setObject(5, transactionTime);
                activate.setObject(6, transactionTime);
                if (activate.executeUpdate() != 1) {
                    throw new IndexingException(
                            "DOCUMENT_VERSION_NOT_PUBLISHED",
                            "document version is no longer the active published version"
                    );
                }
            }
            connection.commit();
        } catch (IndexingException error) {
            rollback(connection, error);
            throw error;
        } catch (Exception error) {
            rollback(connection, error);
            throw new IndexTaskPersistenceException("failed to replace indexed document version", error);
        } finally {
            close(connection);
        }
    }

    private static void assertCurrentLease(
            Connection connection,
            ClaimedIndexTask task,
            OffsetDateTime transactionTime
    ) throws Exception {
        try (var fence = connection.prepareStatement(FENCE_SQL)) {
            fence.setObject(1, task.taskId());
            fence.setString(2, task.tenantId().value());
            fence.setString(3, task.documentId().value());
            fence.setInt(4, task.documentVersion());
            fence.setString(5, task.taskType().name());
            fence.setInt(6, task.leaseAttempt());
            fence.setObject(7, transactionTime);
            try (var result = fence.executeQuery()) {
                if (!result.next()) {
                    throw new IndexingException(
                            "INDEX_TASK_LEASE_LOST",
                            "index task lease is no longer current"
                    );
                }
            }
        }
    }

    private static void validate(
            ClaimedIndexTask task,
            List<DocumentChunk> chunks,
            List<KnowledgeEmbedding> embeddings
    ) {
        if (chunks == null || chunks.isEmpty()) throw new IllegalArgumentException("chunks must not be empty");
        if (embeddings == null || chunks.size() != embeddings.size()) {
            throw new IllegalArgumentException("one embedding is required for each chunk");
        }
        for (DocumentChunk chunk : chunks) {
            if (!chunk.tenantId().equals(task.tenantId())
                    || !chunk.documentId().equals(task.documentId())
                    || chunk.documentVersion() != task.documentVersion()) {
                throw new IllegalArgumentException("chunk does not belong to the claimed document version");
            }
        }
    }

    private String json(List<String> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException error) {
            throw new IllegalArgumentException("heading path cannot be serialized", error);
        }
    }

    private static String vectorLiteral(float[] vector) {
        StringJoiner values = new StringJoiner(",", "[", "]");
        for (float value : vector) values.add(Float.toString(value));
        return values.toString();
    }

    private static void rollback(Connection connection, Exception original) {
        if (connection == null) return;
        try {
            connection.rollback();
        } catch (Exception rollbackError) {
            original.addSuppressed(rollbackError);
        }
    }

    private static void close(Connection connection) {
        if (connection == null) return;
        try {
            connection.close();
        } catch (Exception ignored) {
            // The write result is already known; pool eviction handles a broken connection.
        }
    }
}
