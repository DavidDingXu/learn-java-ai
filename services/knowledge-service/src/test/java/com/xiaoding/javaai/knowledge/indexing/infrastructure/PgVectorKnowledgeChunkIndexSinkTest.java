package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.document.application.DocumentChunk;
import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import com.xiaoding.javaai.knowledge.indexing.application.IndexingException;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PgVectorKnowledgeChunkIndexSinkTest {

    private static final Instant NOW = Instant.parse("2026-07-17T02:00:00Z");

    @Test
    void replaces_one_version_in_a_single_transaction_and_binds_vector_metadata() {
        RecordingJdbc jdbc = new RecordingJdbc(true, false, 1);
        PgVectorKnowledgeChunkIndexSink sink = new PgVectorKnowledgeChunkIndexSink(
                jdbc.dataSource(), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        sink.replaceVersion(task(), List.of(chunk()), List.of(new KnowledgeEmbedding(
                new float[]{0.1f, 0.2f}, "embedding-v1")));

        assertThat(jdbc.calls).containsSubsequence(
                "connection.setAutoCommit(false)",
                "fence.setObject(1,00000000-0000-0000-0000-000000000020)",
                "fence.setString(2,tenant-a)",
                "fence.setString(3,refund-policy)",
                "fence.setInt(4,1)",
                "fence.setString(5,REINDEX_DOCUMENT_VERSION)",
                "fence.setInt(6,1)",
                "fence.setObject(7,2026-07-17T02:00Z)",
                "fence.executeQuery()",
                "fence.result.next()",
                "delete.setString(1,tenant-a)",
                "delete.setString(2,refund-policy)",
                "delete.setInt(3,1)",
                "delete.executeUpdate()",
                "insert.setString(1,tenant-a)",
                "insert.setString(2,refund-policy)",
                "insert.setInt(3,1)",
                "insert.setString(4,chunk-1)",
                "insert.setString(5,policy-chunk-v1)",
                "insert.setInt(6,1)",
                "insert.setString(7,[\"退款政策\"])",
                "insert.setString(8,第十条)",
                "insert.setString(9,五个工作日内到账。)",
                "insert.setString(10,[0.1,0.2])",
                "insert.setString(11,embedding-v1)",
                "insert.setObject(12,2026-07-17T02:00Z)",
                "insert.addBatch()",
                "insert.executeBatch()",
                "activate.setObject(1,2026-07-17T02:00Z)",
                "activate.setString(2,tenant-a)",
                "activate.setString(3,refund-policy)",
                "activate.setInt(4,1)",
                "activate.setObject(5,2026-07-17T02:00Z)",
                "activate.setObject(6,2026-07-17T02:00Z)",
                "activate.executeUpdate()",
                "connection.commit()"
        );
    }

    @Test
    void rolls_back_when_the_batch_write_fails() {
        RecordingJdbc jdbc = new RecordingJdbc(true, true, 1);
        PgVectorKnowledgeChunkIndexSink sink = new PgVectorKnowledgeChunkIndexSink(
                jdbc.dataSource(), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> sink.replaceVersion(
                task(), List.of(chunk()),
                List.of(new KnowledgeEmbedding(new float[]{0.1f, 0.2f}, "embedding-v1"))
        )).isInstanceOf(IndexTaskPersistenceException.class);

        assertThat(jdbc.calls).contains("connection.rollback()");
        assertThat(jdbc.calls).doesNotContain("connection.commit()");
    }

    @Test
    void rolls_back_chunks_when_the_version_was_replaced_before_search_activation() {
        RecordingJdbc jdbc = new RecordingJdbc(true, false, 0);
        PgVectorKnowledgeChunkIndexSink sink = new PgVectorKnowledgeChunkIndexSink(
                jdbc.dataSource(), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> sink.replaceVersion(
                task(), List.of(chunk()),
                List.of(new KnowledgeEmbedding(new float[]{0.1f, 0.2f}, "embedding-v1"))
        )).isInstanceOfSatisfying(IndexingException.class, error ->
                assertThat(error.errorCode()).isEqualTo("DOCUMENT_VERSION_NOT_PUBLISHED"));

        assertThat(jdbc.calls).contains("activate.executeUpdate()", "connection.rollback()");
        assertThat(jdbc.calls).doesNotContain("connection.commit()");
    }

    @Test
    void rejects_a_lost_lease_before_any_chunk_or_search_version_write() {
        RecordingJdbc jdbc = new RecordingJdbc(false, false, 1);
        PgVectorKnowledgeChunkIndexSink sink = new PgVectorKnowledgeChunkIndexSink(
                jdbc.dataSource(), new ObjectMapper(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> sink.replaceVersion(
                task(), List.of(chunk()),
                List.of(new KnowledgeEmbedding(new float[]{0.1f, 0.2f}, "embedding-v1"))
        )).isInstanceOfSatisfying(IndexingException.class, error ->
                assertThat(error.errorCode()).isEqualTo("INDEX_TASK_LEASE_LOST"));

        assertThat(jdbc.calls).contains("fence.executeQuery()", "fence.result.next()", "connection.rollback()");
        assertThat(jdbc.calls).noneMatch(call -> call.startsWith("delete."));
        assertThat(jdbc.calls).noneMatch(call -> call.startsWith("insert."));
        assertThat(jdbc.calls).noneMatch(call -> call.startsWith("activate."));
        assertThat(jdbc.calls).doesNotContain("connection.commit()");
    }

    private static ClaimedIndexTask task() {
        return new ClaimedIndexTask(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                new TenantId("tenant-a"), new DocumentId("refund-policy"),
                1, IndexTaskType.REINDEX_DOCUMENT_VERSION, 1
        );
    }

    private static DocumentChunk chunk() {
        return new DocumentChunk(
                "chunk-1", new TenantId("tenant-a"), new DocumentId("refund-policy"),
                1, "policy-chunk-v1", 1, List.of("退款政策"), "第十条", "五个工作日内到账。"
        );
    }

    private static final class RecordingJdbc {
        private final List<String> calls = new ArrayList<>();
        private final boolean leaseValid;
        private final boolean failBatch;
        private final int activationRows;
        private int statements;

        private RecordingJdbc(boolean leaseValid, boolean failBatch, int activationRows) {
            this.leaseValid = leaseValid;
            this.failBatch = failBatch;
            this.activationRows = activationRows;
        }

        private DataSource dataSource() {
            Connection connection = proxy(Connection.class, (target, method, arguments) -> {
                if (method.getName().equals("setAutoCommit")) {
                    calls.add("connection.setAutoCommit(" + arguments[0] + ")");
                    return null;
                }
                if (method.getName().equals("prepareStatement")) {
                    String label = switch (statements++) {
                        case 0 -> "fence";
                        case 1 -> "delete";
                        case 2 -> "insert";
                        default -> "activate";
                    };
                    return statement(label);
                }
                if (method.getName().equals("commit") || method.getName().equals("rollback")) {
                    calls.add("connection." + method.getName() + "()");
                    return null;
                }
                return defaultValue(method.getReturnType());
            });
            return proxy(DataSource.class, (target, method, arguments) ->
                    method.getName().equals("getConnection")
                            ? connection
                            : defaultValue(method.getReturnType()));
        }

        private PreparedStatement statement(String label) {
            return proxy(PreparedStatement.class, (target, method, arguments) -> {
                String name = method.getName();
                if (name.startsWith("set")) {
                    calls.add(label + "." + name + "(" + arguments[0] + "," + arguments[1] + ")");
                    return null;
                }
                if (name.equals("executeQuery")) {
                    calls.add(label + ".executeQuery()");
                    return proxy(ResultSet.class, (resultTarget, resultMethod, resultArguments) -> {
                        if (resultMethod.getName().equals("next")) {
                            calls.add(label + ".result.next()");
                            return leaseValid;
                        }
                        return defaultValue(resultMethod.getReturnType());
                    });
                }
                if (name.equals("addBatch") || name.equals("executeUpdate") || name.equals("executeBatch")) {
                    calls.add(label + "." + name + "()");
                    if (name.equals("executeBatch") && failBatch) throw new SQLException("write failed");
                    if (name.equals("executeUpdate")) return label.equals("activate") ? activationRows : 1;
                    if (name.equals("executeBatch")) return new int[]{1};
                    return null;
                }
                return defaultValue(method.getReturnType());
            });
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
        }

        private static Object defaultValue(Class<?> type) {
            if (!type.isPrimitive()) return null;
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0f;
            if (type == double.class) return 0.0d;
            if (type == char.class) return '\0';
            return null;
        }
    }
}
