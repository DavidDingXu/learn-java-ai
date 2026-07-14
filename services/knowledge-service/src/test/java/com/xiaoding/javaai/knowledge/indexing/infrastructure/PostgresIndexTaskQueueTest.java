package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgresIndexTaskQueueTest {

    private static final Instant NOW = Instant.parse("2026-07-13T06:00:00Z");

    @Test
    void maps_the_atomically_claimed_row_to_a_fenced_task() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getObject("task_id", UUID.class))
                .thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        when(resultSet.getString("tenant_id")).thenReturn("tenant-a");
        when(resultSet.getString("document_id")).thenReturn("refund-policy");
        when(resultSet.getInt("version_number")).thenReturn(2);
        when(resultSet.getString("task_type")).thenReturn("REINDEX_DOCUMENT_VERSION");
        when(resultSet.getInt("attempts")).thenReturn(3);

        var claimed = new PostgresIndexTaskQueue(dataSource).claimNext(
                "indexer-a", NOW, Duration.ofSeconds(45), 5
        ).orElseThrow();

        assertThat(claimed.tenantId().value()).isEqualTo("tenant-a");
        assertThat(claimed.documentId().value()).isEqualTo("refund-policy");
        assertThat(claimed.documentVersion()).isEqualTo(2);
        assertThat(claimed.taskType()).isEqualTo(IndexTaskType.REINDEX_DOCUMENT_VERSION);
        assertThat(claimed.leaseAttempt()).isEqualTo(3);
    }

    @Test
    void rejects_completion_when_the_worker_no_longer_owns_the_lease() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(0);

        assertThatThrownBy(() -> new PostgresIndexTaskQueue(dataSource).complete(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                "indexer-a",
                2,
                NOW
        )).isInstanceOf(IndexTaskLeaseLostException.class);
    }
}
