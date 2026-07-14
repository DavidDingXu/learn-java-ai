package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.ticket.agent.application.ConfirmationDecisionReceipt;
import com.xiaoding.javaai.ticket.agent.application.ConfirmationIdempotencyConflictException;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;
import com.xiaoding.javaai.ticket.task.AgentTaskRequest;
import com.xiaoding.javaai.ticket.task.IdempotencyConflictException;
import com.xiaoding.javaai.ticket.task.JdbcAgentTaskRepository;
import com.xiaoding.javaai.ticket.task.OptimisticTaskLockException;
import org.flywaydb.core.Flyway;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcAgentPersistenceTest {

    private JdbcAgentTaskRepository tasks;
    private JdbcAgentAuditTrail auditTrail;
    private JdbcConfirmationDecisionStore decisions;
    private JdbcTemplate jdbc;
    private DelegatedTicketIdentity identity;
    private AgentTaskRequest request;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:agent-" + System.nanoTime()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        Flyway.configure().dataSource(dataSource).load().migrate();

        jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        tasks = new JdbcAgentTaskRepository(jdbc, transactions, objectMapper);
        auditTrail = new JdbcAgentAuditTrail(jdbc, transactions);
        decisions = new JdbcConfirmationDecisionStore(jdbc, transactions);
        identity = new DelegatedTicketIdentity(
                "tenant-a", "customer-1", "agent-bff", List.of("CUSTOMER"), List.of("support"));
        request = new AgentTaskRequest("CASE-1", "查询退款到账时间", Map.of("orderId", "O-1"));
    }

    @Test
    void persistsScopedIdempotencyAndTaskStateWithOptimisticLocking() {
        Instant acceptedAt = Instant.parse("2026-07-14T01:00:00Z");
        AgentTaskRepository.TaskAcceptance first = tasks.accept(
                identity, "idem-1", "fingerprint-a",
                () -> AgentTask.accepted("task-1", identity, request, acceptedAt));
        AgentTaskRepository.TaskAcceptance duplicate = tasks.accept(
                identity, "idem-1", "fingerprint-a",
                () -> AgentTask.accepted("task-other", identity, request, acceptedAt));

        assertFalse(first.duplicate());
        assertTrue(duplicate.duplicate());
        assertEquals("task-1", duplicate.task().taskId());
        assertThrows(IdempotencyConflictException.class, () -> tasks.accept(
                identity, "idem-1", "fingerprint-b",
                () -> AgentTask.accepted("task-other", identity, request, acceptedAt)));

        AgentTask running = first.task().start(acceptedAt.plusSeconds(1));
        tasks.save(running, 0);
        assertEquals(AgentTaskState.RUNNING, tasks.findById("task-1").orElseThrow().state());
        assertThrows(OptimisticTaskLockException.class, () -> tasks.save(running, 0));
    }

    @Test
    void persistsOrderedAuditEventsPerTask() {
        tasks.accept(identity, "idem-2", "fingerprint-a",
                () -> AgentTask.accepted("task-2", identity, request, Instant.parse("2026-07-14T02:00:00Z")));

        auditTrail.append("task-2", "TASK_ACCEPTED", "agent-bff", "accepted",
                Instant.parse("2026-07-14T02:00:00Z"));
        auditTrail.append("task-2", "TASK_STARTED", "ticket-agent", "started",
                Instant.parse("2026-07-14T02:00:01Z"));

        assertEquals(List.of(1L, 2L), auditTrail.findByTaskId("task-2").stream()
                .map(event -> event.sequence()).toList());
    }

    @Test
    void persistsConfirmationDecisionAndReturnsDuplicateReceiptWithoutRepeatingAction() {
        AtomicInteger executions = new AtomicInteger();
        ConfirmationDecisionReceipt receipt = new ConfirmationDecisionReceipt(
                "task-3", AgentTaskState.COMPLETED, "action-1", "SUCCEEDED", "audit-1", 4, false);

        var first = decisions.executeOnce("tenant-a\noperator-1", "confirm-1", "fingerprint-a", () -> {
            executions.incrementAndGet();
            return receipt;
        });
        var duplicate = decisions.executeOnce(
                "tenant-a\noperator-1", "confirm-1", "fingerprint-a", () -> {
                    executions.incrementAndGet();
                    return receipt;
                });

        assertFalse(first.duplicate());
        assertTrue(duplicate.duplicate());
        assertEquals(1, executions.get());
        assertThrows(ConfirmationIdempotencyConflictException.class, () -> decisions.executeOnce(
                "tenant-a\noperator-1", "confirm-1", "fingerprint-b", () -> receipt));
    }

    @Test
    void doesNotAutomaticallyReclaimAnExpiredWriteExecution() {
        Instant now = Instant.parse("2026-07-14T03:00:00Z");
        jdbc.update("""
                        insert into agent_confirmation_decision (
                            principal_scope, idempotency_key, request_fingerprint, decision_status,
                            lease_owner, lease_until, created_at, updated_at
                        ) values (?, ?, ?, 'PENDING', ?, ?, ?, ?)
                        """,
                "tenant-a\noperator-1", "confirm-stale", "fingerprint-a", "owner-crashed",
                Timestamp.from(now.minusSeconds(30)), Timestamp.from(now.minusSeconds(60)),
                Timestamp.from(now.minusSeconds(60)));
        JdbcConfirmationDecisionStore staleStore = new JdbcConfirmationDecisionStore(
                jdbc,
                new TransactionTemplate(new DataSourceTransactionManager(jdbc.getDataSource())),
                Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofSeconds(30));
        AtomicInteger executions = new AtomicInteger();

        assertThrows(ConfirmationDecisionInProgressException.class, () -> staleStore.executeOnce(
                "tenant-a\noperator-1", "confirm-stale", "fingerprint-a", () -> {
                    executions.incrementAndGet();
                    return new ConfirmationDecisionReceipt(
                            "task-3", AgentTaskState.COMPLETED, "action-1",
                            "SUCCEEDED", "audit-1", 4, false);
                }));
        assertEquals(0, executions.get());
    }
}
