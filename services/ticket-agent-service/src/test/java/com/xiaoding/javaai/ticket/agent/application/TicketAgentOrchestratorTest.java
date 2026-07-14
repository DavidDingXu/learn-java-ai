package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.agent.domain.ToolRisk;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskIntakeService;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;
import com.xiaoding.javaai.ticket.task.AgentTaskRequest;
import com.xiaoding.javaai.ticket.task.InMemoryAgentTaskRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class TicketAgentOrchestratorTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void executes_a_bounded_read_tool_then_finishes_with_a_traceable_outcome() {
        InMemoryAgentTaskRepository repository = acceptedTask();
        ArrayDeque<AgentDecision> decisions = new ArrayDeque<>(List.of(
                new AgentDecision.UseTool(
                        "QUERY_KNOWLEDGE", Map.of("question", "退款多久到账？"), "查询当前制度"),
                new AgentDecision.Finish("已根据当前退款制度生成客服处理建议")
        ));
        TicketAgentPlanner planner = context -> AgentPlanningResult.decisionOnly(decisions.removeFirst());
        InMemoryAgentAuditTrail audit = new InMemoryAgentAuditTrail();
        AgentReadToolExecutor executor = (call, task) -> {
            assertThat(task.identity().tenantId()).isEqualTo("tenant-a");
            assertThat(call.arguments()).doesNotContainKeys("tenantId", "roles");
            return new ToolObservation(
                    call.toolName(),
                    Map.of("answer", "退款通常在原路退回后到账", "citation", "refund-policy/v1#arrival-time"),
                    CLOCK.instant());
        };
        TicketAgentOrchestrator orchestrator = orchestrator(repository, planner, executor, audit);

        AgentTask result = orchestrator.run("task-100");

        assertThat(result.state()).isEqualTo(AgentTaskState.COMPLETED);
        assertThat(result.observations()).singleElement()
                .satisfies(observation -> assertThat(observation.toolName()).isEqualTo("QUERY_KNOWLEDGE"));
        assertThat(result.outcome()).contains("退款制度");
        assertThat(audit.findByTaskId("task-100"))
                .extracting(AgentAuditEvent::eventType)
                .containsExactly(
                        "AGENT_RUN_STARTED",
                        "AGENT_PLAN_RECORDED",
                        "READ_TOOL_SUCCEEDED",
                        "AGENT_PLAN_RECORDED",
                        "AGENT_TASK_COMPLETED");
        assertThat(audit.findByTaskId("task-100").get(1).detail())
                .contains("model=test-double")
                .doesNotContain("退款多久到账");
    }

    @Test
    void turns_a_write_tool_selection_into_a_version_bound_confirmation() {
        InMemoryAgentTaskRepository repository = acceptedTask();
        TicketAgentPlanner planner = context -> AgentPlanningResult.decisionOnly(new AgentDecision.UseTool(
                "ASSIGN_QUEUE", Map.of("queueCode", "refund-review"), "需要退款专席继续处理"));
        AgentReadToolExecutor executor = (call, task) -> fail("write tools must not execute before confirmation");
        TicketAgentOrchestrator orchestrator = orchestrator(repository, planner, executor);

        AgentTask result = orchestrator.run("task-100");

        assertThat(result.state()).isEqualTo(AgentTaskState.WAITING_CONFIRMATION);
        assertThat(result.confirmation()).isNotNull();
        assertThat(result.confirmation().risk()).isEqualTo(ToolRisk.MEDIUM);
        assertThat(result.confirmation().requiredRole()).isEqualTo("TICKET_OPERATOR");
        assertThat(result.confirmation().boundTaskVersion()).isEqualTo(result.version());
        assertThat(result.confirmation().arguments())
                .containsExactlyEntriesOf(Map.of("queueCode", "refund-review"));
    }

    @Test
    void records_a_business_refusal_without_calling_any_tool() {
        InMemoryAgentTaskRepository repository = acceptedTask();
        TicketAgentPlanner planner = context -> AgentPlanningResult.decisionOnly(new AgentDecision.Refuse(
                "OUT_OF_SCOPE", "该任务不属于客服工单处理范围"));
        AgentReadToolExecutor executor = (call, task) -> fail("refusal must not execute a tool");

        AgentTask result = orchestrator(repository, planner, executor).run("task-100");

        assertThat(result.state()).isEqualTo(AgentTaskState.REJECTED);
        assertThat(result.outcome()).contains("OUT_OF_SCOPE");
    }

    @Test
    void fails_closed_when_the_planner_exceeds_the_step_budget() {
        InMemoryAgentTaskRepository repository = acceptedTask();
        TicketAgentPlanner planner = context -> AgentPlanningResult.decisionOnly(new AgentDecision.UseTool(
                "QUERY_KNOWLEDGE", Map.of("question", "退款多久到账？"), "继续查询"));
        AgentReadToolExecutor executor = (call, task) -> new ToolObservation(
                call.toolName(), Map.of("answer", "same"), CLOCK.instant());

        AgentTask result = new TicketAgentOrchestrator(
                repository,
                planner,
                BusinessToolCatalog.standard(Set.of("refund-review")),
                executor,
                new InMemoryAgentAuditTrail(),
                () -> "action-100",
                () -> "confirmation-100",
                CLOCK,
                Duration.ofMinutes(15),
                2).run("task-100");

        assertThat(result.state()).isEqualTo(AgentTaskState.FAILED);
        assertThat(result.outcome()).contains("STEP_BUDGET_EXCEEDED");
    }

    private static TicketAgentOrchestrator orchestrator(
            AgentTaskRepository repository,
            TicketAgentPlanner planner,
            AgentReadToolExecutor executor
    ) {
        return orchestrator(repository, planner, executor, new InMemoryAgentAuditTrail());
    }

    private static TicketAgentOrchestrator orchestrator(
            AgentTaskRepository repository,
            TicketAgentPlanner planner,
            AgentReadToolExecutor executor,
            AgentAuditTrail auditTrail
    ) {
        return new TicketAgentOrchestrator(
                repository,
                planner,
                BusinessToolCatalog.standard(Set.of("refund-review", "tier-2")),
                executor,
                auditTrail,
                () -> "action-100",
                () -> "confirmation-100",
                CLOCK,
                Duration.ofMinutes(15),
                4);
    }

    private static InMemoryAgentTaskRepository acceptedTask() {
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        AgentTaskIntakeService intake = new AgentTaskIntakeService(() -> "task-100", repository, CLOCK);
        intake.accept(
                new DelegatedTicketIdentity(
                        "tenant-a", "customer-42", "customer-bff",
                        List.of("CUSTOMER"), List.of("retail")),
                "handoff:tenant-a:conversation-1:attempt-1",
                new AgentTaskRequest(
                        "conversation-1",
                        "Resolve customer consultation",
                        Map.of("question", "退款多久到账？", "sourceTraceId", "trace-123")));
        return repository;
    }
}
