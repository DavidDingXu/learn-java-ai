package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationDecision;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.agent.domain.ToolRisk;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;
import com.xiaoding.javaai.ticket.task.AgentTaskRequest;
import com.xiaoding.javaai.ticket.task.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolConfirmationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T08:10:00Z"), ZoneOffset.UTC);

    @Test
    void executes_an_approved_action_once_and_returns_the_original_result_for_a_duplicate() {
        InMemoryAgentTaskRepository tasks = waitingTask(Instant.parse("2026-07-13T08:20:00Z"));
        InMemoryConfirmationDecisionStore decisions = new InMemoryConfirmationDecisionStore();
        InMemoryAgentAuditTrail audit = new InMemoryAgentAuditTrail();
        AtomicInteger executions = new AtomicInteger();
        LegacyWriteToolExecutor executor = (task, confirmation, key) -> {
            executions.incrementAndGet();
            assertThat(key).isEqualTo("tool:tenant-a:action-100");
            return new ToolExecutionReceipt("action-100", "SUCCEEDED", false, "legacy-audit-100");
        };
        ToolConfirmationService service = new ToolConfirmationService(
                tasks, decisions, executor, audit, CLOCK);
        ConfirmationActor actor = operator("tenant-a", "TICKET_OPERATOR");
        ConfirmToolAction command = new ConfirmToolAction(
                "confirmation-100", 2, ConfirmationDecision.APPROVE, "已核对客户诉求");

        ConfirmationDecisionReceipt first = service.decide(
                "task-100", actor, "confirm:task-100:decision-1", command);
        ConfirmationDecisionReceipt duplicate = service.decide(
                "task-100", actor, "confirm:task-100:decision-1", command);

        assertThat(executions).hasValue(1);
        assertThat(first.state()).isEqualTo(AgentTaskState.COMPLETED);
        assertThat(first.duplicate()).isFalse();
        assertThat(duplicate.state()).isEqualTo(AgentTaskState.COMPLETED);
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(tasks.findById("task-100").orElseThrow().state())
                .isEqualTo(AgentTaskState.COMPLETED);
        assertThat(audit.findByTaskId("task-100"))
                .extracting(event -> event.eventType())
                .containsExactly("CONFIRMATION_APPROVED", "TOOL_EXECUTION_SUCCEEDED");
    }

    @Test
    void rejects_reusing_a_confirmation_idempotency_key_for_a_different_decision() {
        InMemoryAgentTaskRepository tasks = waitingTask(Instant.parse("2026-07-13T08:20:00Z"));
        ToolConfirmationService service = service(tasks, (task, confirmation, key) ->
                new ToolExecutionReceipt("action-100", "SUCCEEDED", false, "audit-100"));
        ConfirmationActor actor = operator("tenant-a", "TICKET_OPERATOR");
        String key = "confirm:task-100:decision-1";
        service.decide("task-100", actor, key, new ConfirmToolAction(
                "confirmation-100", 2, ConfirmationDecision.REJECT, "不应调整队列"));

        assertThatThrownBy(() -> service.decide("task-100", actor, key, new ConfirmToolAction(
                "confirmation-100", 2, ConfirmationDecision.APPROVE, "改为同意")))
                .isInstanceOf(ConfirmationIdempotencyConflictException.class);
    }

    @Test
    void scopes_confirmation_idempotency_to_the_trusted_operator_identity() {
        InMemoryAgentTaskRepository tasks = waitingTask(Instant.parse("2026-07-13T08:20:00Z"));
        ToolConfirmationService service = service(tasks, (task, confirmation, key) ->
                new ToolExecutionReceipt("action-100", "SUCCEEDED", false, "audit-100"));
        String key = "confirm:task-100:decision-1";
        ConfirmToolAction command = new ConfirmToolAction(
                "confirmation-100", 2, ConfirmationDecision.APPROVE, "approve");
        service.decide("task-100", operator("tenant-a", "employee-7", "TICKET_OPERATOR"), key, command);

        assertThatThrownBy(() -> service.decide(
                "task-100", operator("tenant-a", "employee-8", "TICKET_OPERATOR"), key, command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("waiting for confirmation");
    }

    @Test
    void rejects_an_expired_stale_or_unauthorized_confirmation_before_execution() {
        AtomicInteger executions = new AtomicInteger();
        LegacyWriteToolExecutor executor = (task, confirmation, key) -> {
            executions.incrementAndGet();
            return new ToolExecutionReceipt("action-100", "SUCCEEDED", false, "audit-100");
        };

        ToolConfirmationService expiredService = service(
                waitingTask(Instant.parse("2026-07-13T08:09:59Z")), executor);
        assertThatThrownBy(() -> expiredService.decide(
                "task-100", operator("tenant-a", "TICKET_OPERATOR"), "confirm:expired",
                new ConfirmToolAction("confirmation-100", 2,
                        ConfirmationDecision.APPROVE, "approve")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");

        ToolConfirmationService unauthorizedService = service(
                waitingTask(Instant.parse("2026-07-13T08:20:00Z")), executor);
        assertThatThrownBy(() -> unauthorizedService.decide(
                "task-100", operator("tenant-a", "CUSTOMER"), "confirm:unauthorized",
                new ConfirmToolAction("confirmation-100", 2,
                        ConfirmationDecision.APPROVE, "approve")))
                .isInstanceOf(com.xiaoding.javaai.ticket.task.AgentTaskAccessDeniedException.class)
                .hasMessageContaining("role");

        ToolConfirmationService staleService = service(
                waitingTask(Instant.parse("2026-07-13T08:20:00Z")), executor);
        assertThatThrownBy(() -> staleService.decide(
                "task-100", operator("tenant-a", "TICKET_OPERATOR"), "confirm:stale",
                new ConfirmToolAction("confirmation-100", 1,
                        ConfirmationDecision.APPROVE, "approve")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version");

        assertThat(executions).hasValue(0);
    }

    @Test
    void stops_in_execution_uncertain_after_a_remote_timeout_and_does_not_auto_retry() {
        InMemoryAgentTaskRepository tasks = waitingTask(Instant.parse("2026-07-13T08:20:00Z"));
        AtomicInteger executions = new AtomicInteger();
        LegacyWriteToolExecutor executor = (task, confirmation, key) -> {
            executions.incrementAndGet();
            throw new RemoteExecutionUncertainException("legacy request timed out after send");
        };
        ToolConfirmationService service = service(tasks, executor);
        ConfirmToolAction command = new ConfirmToolAction(
                "confirmation-100", 2, ConfirmationDecision.APPROVE, "approve");

        ConfirmationDecisionReceipt first = service.decide(
                "task-100", operator("tenant-a", "TICKET_OPERATOR"), "confirm:uncertain", command);
        ConfirmationDecisionReceipt duplicate = service.decide(
                "task-100", operator("tenant-a", "TICKET_OPERATOR"), "confirm:uncertain", command);

        assertThat(executions).hasValue(1);
        assertThat(first.state()).isEqualTo(AgentTaskState.EXECUTION_UNCERTAIN);
        assertThat(duplicate.duplicate()).isTrue();
    }

    @Test
    void marks_the_task_failed_when_the_legacy_system_explicitly_rejects_the_action() {
        InMemoryAgentTaskRepository tasks = waitingTask(Instant.parse("2026-07-13T08:20:00Z"));
        InMemoryAgentAuditTrail audit = new InMemoryAgentAuditTrail();
        ToolConfirmationService service = new ToolConfirmationService(
                tasks,
                new InMemoryConfirmationDecisionStore(),
                (task, confirmation, key) -> {
                    throw new ToolExecutionRejectedException("LEGACY_VALIDATION_FAILED", "queue is closed");
                },
                audit,
                CLOCK);

        ConfirmationDecisionReceipt receipt = service.decide(
                "task-100",
                operator("tenant-a", "TICKET_OPERATOR"),
                "confirm:rejected",
                new ConfirmToolAction(
                        "confirmation-100", 2, ConfirmationDecision.APPROVE, "approve"));

        assertThat(receipt.state()).isEqualTo(AgentTaskState.FAILED);
        assertThat(receipt.toolStatus()).isEqualTo("REJECTED");
        assertThat(tasks.findById("task-100").orElseThrow().state()).isEqualTo(AgentTaskState.FAILED);
        assertThat(audit.findByTaskId("task-100"))
                .extracting(AgentAuditEvent::eventType)
                .containsExactly("CONFIRMATION_APPROVED", "TOOL_EXECUTION_REJECTED");
    }

    @Test
    void moves_out_of_executing_before_propagating_an_unexpected_local_failure() {
        InMemoryAgentTaskRepository tasks = waitingTask(Instant.parse("2026-07-13T08:20:00Z"));
        InMemoryAgentAuditTrail audit = new InMemoryAgentAuditTrail();
        ToolConfirmationService service = new ToolConfirmationService(
                tasks,
                new InMemoryConfirmationDecisionStore(),
                (task, confirmation, key) -> {
                    throw new IllegalStateException("token provider is unavailable");
                },
                audit,
                CLOCK);

        assertThatThrownBy(() -> service.decide(
                "task-100",
                operator("tenant-a", "TICKET_OPERATOR"),
                "confirm:local-failure",
                new ConfirmToolAction(
                        "confirmation-100", 2, ConfirmationDecision.APPROVE, "approve")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("token provider");

        assertThat(tasks.findById("task-100").orElseThrow().state()).isEqualTo(AgentTaskState.FAILED);
        assertThat(audit.findByTaskId("task-100"))
                .extracting(AgentAuditEvent::eventType)
                .containsExactly("CONFIRMATION_APPROVED", "TOOL_EXECUTION_FAILED");
    }

    private static ToolConfirmationService service(
            AgentTaskRepository tasks,
            LegacyWriteToolExecutor executor
    ) {
        return new ToolConfirmationService(
                tasks,
                new InMemoryConfirmationDecisionStore(),
                executor,
                new InMemoryAgentAuditTrail(),
                CLOCK);
    }

    private static InMemoryAgentTaskRepository waitingTask(Instant expiresAt) {
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        DelegatedTicketIdentity identity = new DelegatedTicketIdentity(
                "tenant-a", "customer-42", "customer-bff", List.of("CUSTOMER"), List.of("retail"));
        AgentTask accepted = AgentTask.accepted(
                "task-100", identity,
                new AgentTaskRequest("case-1", "assign ticket", Map.of("question", "refund")),
                Instant.parse("2026-07-13T08:00:00Z"));
        repository.accept(identity, "handoff-key", "fingerprint", () -> accepted);
        AgentTask running = repository.save(
                accepted.start(Instant.parse("2026-07-13T08:01:00Z")), 0);
        ConfirmationRequest confirmation = new ConfirmationRequest(
                "confirmation-100",
                "action-100",
                "ASSIGN_QUEUE",
                ToolRisk.MEDIUM,
                "TICKET_OPERATOR",
                Map.of("queueCode", "refund-review"),
                "action-fingerprint",
                2,
                expiresAt);
        repository.save(running.waitForConfirmation(
                confirmation, Instant.parse("2026-07-13T08:02:00Z")), 1);
        return repository;
    }

    private static ConfirmationActor operator(String tenantId, String role) {
        return operator(tenantId, "employee-7", role);
    }

    private static ConfirmationActor operator(String tenantId, String subjectId, String role) {
        return new ConfirmationActor(
                tenantId, subjectId, "jdk8-crm", List.of(role));
    }
}
