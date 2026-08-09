package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskLeaseLostException;
import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskType;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PostgresIndexTaskQueue implements IndexTaskQueue {

    private final DataSource dataSource;

    public PostgresIndexTaskQueue(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<ClaimedIndexTask> claimNext(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    ) {
        return claim(PostgresIndexTaskClaimQuery.create(
                workerId, now, leaseDuration, maximumAttempts
        ));
    }

    @Override
    public Optional<ClaimedIndexTask> claimNextForTenant(
            TenantId tenantId,
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    ) {
        return claim(PostgresIndexTaskClaimQuery.createForTenant(
                tenantId, workerId, now, leaseDuration, maximumAttempts
        ));
    }

    @Override
    public void renewLease(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now,
            Duration leaseDuration
    ) {
        executeMutation(
                PostgresIndexTaskMutationQuery.renewLease(
                        taskId, workerId, leaseAttempt, now, leaseDuration
                ),
                taskId,
                workerId,
                leaseAttempt
        );
    }

    private Optional<ClaimedIndexTask> claim(PostgresIndexTaskClaimQuery query) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(query.sql())) {
            bind(statement, query.parameters());
            try (var resultSet = statement.executeQuery()) {
                if (!resultSet.next()) return Optional.empty();
                return Optional.of(map(resultSet));
            }
        } catch (Exception error) {
            throw new IndexTaskPersistenceException("failed to claim index task", error);
        }
    }

    @Override
    public void complete(UUID taskId, String workerId, int leaseAttempt, Instant now) {
        executeMutation(
                PostgresIndexTaskMutationQuery.complete(taskId, workerId, leaseAttempt, now),
                taskId,
                workerId,
                leaseAttempt
        );
    }

    @Override
    public void fail(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now,
            String errorCode,
            Duration retryDelay,
            int maximumAttempts
    ) {
        executeMutation(
                PostgresIndexTaskMutationQuery.fail(
                        taskId, workerId, leaseAttempt, now, errorCode, retryDelay, maximumAttempts
                ),
                taskId,
                workerId,
                leaseAttempt
        );
    }

    private void executeMutation(
            PostgresIndexTaskMutationQuery query,
            UUID taskId,
            String workerId,
            int leaseAttempt
    ) {
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement(query.sql())) {
            bind(statement, query.parameters());
            if (statement.executeUpdate() != 1) {
                throw new IndexTaskLeaseLostException(taskId, workerId, leaseAttempt);
            }
        } catch (IndexTaskLeaseLostException error) {
            throw error;
        } catch (Exception error) {
            throw new IndexTaskPersistenceException("failed to mutate index task", error);
        }
    }

    private static ClaimedIndexTask map(ResultSet resultSet) throws SQLException {
        return new ClaimedIndexTask(
                resultSet.getObject("task_id", UUID.class),
                new TenantId(resultSet.getString("tenant_id")),
                new DocumentId(resultSet.getString("document_id")),
                resultSet.getInt("version_number"),
                IndexTaskType.valueOf(resultSet.getString("task_type")),
                resultSet.getInt("attempts")
        );
    }

    private static void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index += 1) {
            Object value = parameters.get(index);
            int parameterIndex = index + 1;
            if (value instanceof String text) statement.setString(parameterIndex, text);
            else if (value instanceof Instant instant) {
                statement.setObject(parameterIndex, OffsetDateTime.ofInstant(instant, ZoneOffset.UTC));
            } else if (value instanceof Integer integer) statement.setInt(parameterIndex, integer);
            else if (value instanceof UUID uuid) statement.setObject(parameterIndex, uuid);
            else throw new IllegalArgumentException("unsupported index task parameter " + value.getClass().getName());
        }
    }
}
