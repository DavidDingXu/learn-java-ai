package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentAuditEvent;
import com.xiaoding.javaai.ticket.agent.application.AgentAuditTrail;
import com.xiaoding.javaai.ticket.task.AgentTaskNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public final class JdbcAgentAuditTrail implements AgentAuditTrail {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;

    public JdbcAgentAuditTrail(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = java.util.Objects.requireNonNull(jdbc, "jdbc must not be null");
        this.transactions = java.util.Objects.requireNonNull(transactions, "transactions must not be null");
    }

    @Override
    public AgentAuditEvent append(
            String taskId,
            String eventType,
            String actorId,
            String detail,
            Instant occurredAt
    ) {
        return transactions.execute(status -> {
            List<String> locked = jdbc.query(
                    "select task_id from agent_task where task_id = ? for update",
                    (resultSet, rowNumber) -> resultSet.getString("task_id"), taskId);
            if (locked.isEmpty()) throw new AgentTaskNotFoundException(taskId);
            Long sequence = jdbc.queryForObject(
                    "select coalesce(max(sequence_no), 0) + 1 from agent_audit_event where task_id = ?",
                    Long.class, taskId);
            AgentAuditEvent event = new AgentAuditEvent(
                    taskId, sequence, eventType, actorId, detail, occurredAt);
            jdbc.update("""
                            insert into agent_audit_event (
                                task_id, sequence_no, event_type, actor_id, detail, occurred_at
                            ) values (?, ?, ?, ?, ?, ?)
                            """,
                    taskId, sequence, eventType, actorId, detail, Timestamp.from(occurredAt));
            return event;
        });
    }

    @Override
    public List<AgentAuditEvent> findByTaskId(String taskId) {
        return jdbc.query("""
                        select task_id, sequence_no, event_type, actor_id, detail, occurred_at
                          from agent_audit_event
                         where task_id = ?
                         order by sequence_no
                        """,
                (resultSet, rowNumber) -> new AgentAuditEvent(
                        resultSet.getString("task_id"),
                        resultSet.getLong("sequence_no"),
                        resultSet.getString("event_type"),
                        resultSet.getString("actor_id"),
                        resultSet.getString("detail"),
                        resultSet.getTimestamp("occurred_at").toInstant()),
                taskId);
    }
}
