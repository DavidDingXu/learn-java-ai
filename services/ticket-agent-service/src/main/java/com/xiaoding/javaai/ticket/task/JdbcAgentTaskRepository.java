package com.xiaoding.javaai.ticket.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public final class JdbcAgentTaskRepository implements AgentTaskRepository {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final ObjectMapper objectMapper;

    public JdbcAgentTaskRepository(
            JdbcTemplate jdbc,
            TransactionTemplate transactions,
            ObjectMapper objectMapper
    ) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.transactions = java.util.Objects.requireNonNull(transactions, "transactions must not be null");
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public TaskAcceptance accept(
            DelegatedTicketIdentity identity,
            String idempotencyKey,
            String fingerprint,
            Supplier<AgentTask> newTask
    ) {
        try {
            return transactions.execute(status -> {
                AgentTask task = newTask.get();
                jdbc.update("""
                                insert into agent_task (
                                    task_id, tenant_id, subject_id, actor_id, idempotency_key,
                                    request_fingerprint, state, task_version, snapshot_json, created_at, updated_at
                                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                """,
                        task.taskId(), identity.tenantId(), identity.subjectId(), identity.actorId(),
                        idempotencyKey, fingerprint, task.state().name(), task.version(), encode(task),
                        Timestamp.from(task.createdAt()), Timestamp.from(task.updatedAt()));
                return new TaskAcceptance(task, false);
            });
        } catch (DuplicateKeyException duplicate) {
            StoredTask existing = findByScopedIdempotency(identity, idempotencyKey)
                    .orElseThrow(() -> duplicate);
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new IdempotencyConflictException(idempotencyKey);
            }
            return new TaskAcceptance(existing.task(), true);
        }
    }

    @Override
    public Optional<AgentTask> findById(String taskId) {
        return jdbc.query(
                        "select snapshot_json from agent_task where task_id = ?",
                        (resultSet, rowNumber) -> decode(resultSet.getString("snapshot_json")), taskId)
                .stream().findFirst();
    }

    @Override
    public AgentTask save(AgentTask task, long expectedVersion) {
        return transactions.execute(status -> {
            int updated = jdbc.update("""
                            update agent_task
                               set state = ?, task_version = ?, snapshot_json = ?, updated_at = ?
                             where task_id = ? and task_version = ?
                            """,
                    task.state().name(), task.version(), encode(task), Timestamp.from(task.updatedAt()),
                    task.taskId(), expectedVersion);
            if (updated == 1) return task;

            List<Long> versions = jdbc.query(
                    "select task_version from agent_task where task_id = ?",
                    (resultSet, rowNumber) -> resultSet.getLong("task_version"), task.taskId());
            if (versions.isEmpty()) throw new AgentTaskNotFoundException(task.taskId());
            throw new OptimisticTaskLockException(task.taskId(), expectedVersion, versions.getFirst());
        });
    }

    private Optional<StoredTask> findByScopedIdempotency(
            DelegatedTicketIdentity identity,
            String idempotencyKey
    ) {
        return jdbc.query("""
                        select request_fingerprint, snapshot_json
                          from agent_task
                         where tenant_id = ? and subject_id = ? and actor_id = ? and idempotency_key = ?
                        """,
                (resultSet, rowNumber) -> new StoredTask(
                        resultSet.getString("request_fingerprint"),
                        decode(resultSet.getString("snapshot_json"))),
                identity.tenantId(), identity.subjectId(), identity.actorId(), idempotencyKey)
                .stream().findFirst();
    }

    private String encode(AgentTask task) {
        TaskSnapshot snapshot = new TaskSnapshot(
                task.taskId(), task.identity(), task.request(), task.createdAt(), task.state(),
                task.version(), task.observations(), task.confirmation(), task.outcome(), task.updatedAt());
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("cannot serialize agent task " + task.taskId(), error);
        }
    }

    private AgentTask decode(String json) {
        try {
            TaskSnapshot snapshot = objectMapper.readValue(json, TaskSnapshot.class);
            return AgentTask.restore(
                    snapshot.taskId(), snapshot.identity(), snapshot.request(), snapshot.createdAt(),
                    snapshot.state(), snapshot.version(), snapshot.observations(), snapshot.confirmation(),
                    snapshot.outcome(), snapshot.updatedAt());
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("cannot deserialize agent task", error);
        }
    }

    private record StoredTask(String fingerprint, AgentTask task) {
    }

    private record TaskSnapshot(
            String taskId,
            DelegatedTicketIdentity identity,
            AgentTaskRequest request,
            Instant createdAt,
            AgentTaskState state,
            long version,
            List<ToolObservation> observations,
            ConfirmationRequest confirmation,
            String outcome,
            Instant updatedAt
    ) {
    }
}
