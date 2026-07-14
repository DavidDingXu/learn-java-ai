package com.xiaoding.javaai.ticket.agent.web;

import com.xiaoding.javaai.ticket.agent.application.ConfirmToolAction;
import com.xiaoding.javaai.ticket.agent.application.AgentRunAdmission;
import com.xiaoding.javaai.ticket.agent.application.InMemoryAgentAuditTrail;
import com.xiaoding.javaai.ticket.agent.application.InMemoryConfirmationDecisionStore;
import com.xiaoding.javaai.ticket.agent.application.ToolConfirmationService;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationDecision;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.agent.domain.ToolRisk;
import com.xiaoding.javaai.ticket.security.ConfirmationActorFactory;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskAccessDeniedException;
import com.xiaoding.javaai.ticket.task.AgentTaskNotFoundException;
import com.xiaoding.javaai.ticket.task.AgentTaskRequest;
import com.xiaoding.javaai.ticket.task.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTaskWorkflowControllerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T08:10:00Z"), ZoneOffset.UTC);

    @Test
    void exposes_a_versioned_confirmation_and_accepts_an_employee_decision() {
        InMemoryAgentTaskRepository repository = waitingTask();
        InMemoryAgentAuditTrail audit = new InMemoryAgentAuditTrail();
        ToolConfirmationService confirmationService = new ToolConfirmationService(
                repository,
                new InMemoryConfirmationDecisionStore(),
                (task, confirmation, key) -> new ToolExecutionReceipt(
                        confirmation.actionId(), "SUCCEEDED", false, "legacy-audit-100"),
                audit,
                CLOCK);
        AgentTaskWorkflowController controller = new AgentTaskWorkflowController(
                repository,
                taskId -> repository.findById(taskId).orElseThrow(),
                confirmationService::decide,
                audit,
                new ConfirmationActorFactory(),
                AgentRunAdmission.UNLIMITED);

        AgentTaskView before = controller.get(jwt("jdk8-crm"), "task-100");
        AgentTaskView workerView = controller.run(jwt("ticket-agent-worker"), "task-100");
        ConfirmationDecisionReceiptResponse receipt = controller.confirm(
                jwt("jdk8-crm"),
                "task-100",
                "confirm:task-100:decision-1",
                new ConfirmToolActionWebRequest(
                        "confirmation-100", 2, ConfirmationDecision.APPROVE, "已核对工单"));

        assertThat(before.status()).isEqualTo(AgentTaskState.WAITING_CONFIRMATION.name());
        assertThat(before.version()).isEqualTo(2);
        assertThat(before.confirmation().toolName()).isEqualTo("ASSIGN_QUEUE");
        assertThat(before.confirmation().requiredRole()).isEqualTo("TICKET_OPERATOR");
        assertThat(workerView.taskId()).isEqualTo("task-100");
        assertThat(receipt.state()).isEqualTo(AgentTaskState.COMPLETED.name());
        assertThat(controller.audit(jwt("jdk8-crm"), "task-100"))
                .extracting(AgentAuditEventResponse::eventType)
                .containsExactly("CONFIRMATION_APPROVED", "TOOL_EXECUTION_SUCCEEDED");
    }

    @Test
    void distinguishes_missing_tasks_from_cross_tenant_access() {
        InMemoryAgentTaskRepository repository = waitingTask();
        AgentTaskWorkflowController controller = new AgentTaskWorkflowController(
                repository,
                taskId -> repository.findById(taskId).orElseThrow(),
                (taskId, actor, key, command) -> {
                    throw new AssertionError("confirmation must not run");
                },
                new InMemoryAgentAuditTrail(),
                new ConfirmationActorFactory(),
                AgentRunAdmission.UNLIMITED);

        assertThatThrownBy(() -> controller.get(jwt("jdk8-crm", "tenant-a"), "missing-task"))
                .isInstanceOf(AgentTaskNotFoundException.class);
        assertThatThrownBy(() -> controller.get(jwt("jdk8-crm", "tenant-b"), "task-100"))
                .isInstanceOf(AgentTaskAccessDeniedException.class);
    }

    private static InMemoryAgentTaskRepository waitingTask() {
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        DelegatedTicketIdentity identity = new DelegatedTicketIdentity(
                "tenant-a", "customer-42", "customer-bff", List.of(), List.of());
        AgentTask accepted = AgentTask.accepted(
                "task-100", identity,
                new AgentTaskRequest("ticket-900", "assign ticket", Map.of()),
                Instant.parse("2026-07-13T08:00:00Z"));
        repository.accept(identity, "handoff-key", "fingerprint", () -> accepted);
        AgentTask running = repository.save(
                accepted.start(Instant.parse("2026-07-13T08:01:00Z")), 0);
        repository.save(running.waitForConfirmation(new ConfirmationRequest(
                "confirmation-100", "action-100", "ASSIGN_QUEUE",
                ToolRisk.MEDIUM, "TICKET_OPERATOR",
                Map.of("queueCode", "refund-review"), "fingerprint", 2,
                Instant.parse("2026-07-13T08:20:00Z")),
                Instant.parse("2026-07-13T08:02:00Z")), 1);
        return repository;
    }

    private static Jwt jwt(String actor) {
        return jwt(actor, "tenant-a");
    }

    private static Jwt jwt(String actor, String tenantId) {
        return new Jwt(
                "token-value",
                Instant.parse("2026-07-13T08:00:00Z"),
                Instant.parse("2026-07-13T08:30:00Z"),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "employee-7",
                        "tenantId", tenantId,
                        "act", Map.of("sub", actor),
                        "roles", List.of("TICKET_OPERATOR")
                ));
    }
}
