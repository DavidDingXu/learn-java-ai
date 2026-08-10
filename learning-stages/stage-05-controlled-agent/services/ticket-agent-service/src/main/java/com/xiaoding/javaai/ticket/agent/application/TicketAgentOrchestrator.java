package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.PreparedToolCall;
import com.xiaoding.javaai.ticket.agent.domain.ToolEffect;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskNotFoundException;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Supplier;

public final class TicketAgentOrchestrator implements RunAgentTask {

    private final AgentTaskRepository repository;
    private final TicketAgentPlanner planner;
    private final BusinessToolCatalog toolCatalog;
    private final AgentReadToolExecutor readToolExecutor;
    private final AgentAuditTrail auditTrail;
    private final Supplier<String> actionIdGenerator;
    private final Supplier<String> confirmationIdGenerator;
    private final Clock clock;
    private final Duration confirmationTtl;
    private final int maxSteps;
    private final AgentTelemetry telemetry;

    public TicketAgentOrchestrator(
            AgentTaskRepository repository,
            TicketAgentPlanner planner,
            BusinessToolCatalog toolCatalog,
            AgentReadToolExecutor readToolExecutor,
            AgentAuditTrail auditTrail,
            Supplier<String> actionIdGenerator,
            Supplier<String> confirmationIdGenerator,
            Clock clock,
            Duration confirmationTtl,
            int maxSteps
    ) {
        this(repository, planner, toolCatalog, readToolExecutor, auditTrail,
                actionIdGenerator, confirmationIdGenerator, clock, confirmationTtl,
                maxSteps, AgentTelemetry.NOOP);
    }

    public TicketAgentOrchestrator(
            AgentTaskRepository repository,
            TicketAgentPlanner planner,
            BusinessToolCatalog toolCatalog,
            AgentReadToolExecutor readToolExecutor,
            AgentAuditTrail auditTrail,
            Supplier<String> actionIdGenerator,
            Supplier<String> confirmationIdGenerator,
            Clock clock,
            Duration confirmationTtl,
            int maxSteps,
            AgentTelemetry telemetry
    ) {
        this.repository = repository;
        this.planner = planner;
        this.toolCatalog = toolCatalog;
        this.readToolExecutor = readToolExecutor;
        this.auditTrail = auditTrail;
        this.actionIdGenerator = actionIdGenerator;
        this.confirmationIdGenerator = confirmationIdGenerator;
        this.clock = clock;
        this.confirmationTtl = confirmationTtl;
        this.telemetry = java.util.Objects.requireNonNull(telemetry, "telemetry must not be null");
        if (maxSteps < 1 || maxSteps > 16) throw new IllegalArgumentException("maxSteps must be between 1 and 16");
        this.maxSteps = maxSteps;
    }

    @Override
    public AgentTask run(String taskId) {
        AgentTask task = repository.findById(taskId)
                .orElseThrow(() -> new AgentTaskNotFoundException(taskId));
        if (task.state() == AgentTaskState.ACCEPTED) {
            task = save(task.start(clock.instant()), task.version());
        }
        if (task.state() != AgentTaskState.RUNNING) {
            throw new IllegalStateException("agent task must be RUNNING: " + task.state());
        }
        auditTrail.append(
                task.taskId(), "AGENT_RUN_STARTED", "ticket-agent-worker",
                "taskVersion=" + task.version(), clock.instant());
        for (int step = 0; step < maxSteps; step++) {
            AgentPlanningResult planning = plan(task, step);
            AgentDecision decision = planning.decision();
            recordPlanSafely(planning);
            auditTrail.append(
                    task.taskId(), "AGENT_PLAN_RECORDED", "ticket-agent-worker",
                    "step=" + step
                            + ", decision=" + decisionType(decision)
                            + ", model=" + planning.model()
                            + ", totalTokens=" + planning.usage().totalTokens()
                            + ", finishReason=" + planning.finishReason(),
                    clock.instant());
            if (decision instanceof AgentDecision.Finish finish) {
                AgentTask completed = save(task.complete(finish.summary(), clock.instant()), task.version());
                auditTrail.append(
                        task.taskId(), "AGENT_TASK_COMPLETED", "ticket-agent-worker",
                        "taskVersion=" + completed.version(), clock.instant());
                return completed;
            }
            if (decision instanceof AgentDecision.Refuse refusal) {
                AgentTask rejected = save(task.reject(
                        refusal.reasonCode() + ": " + refusal.message(), clock.instant()), task.version());
                auditTrail.append(
                        task.taskId(), "AGENT_TASK_REJECTED", "ticket-agent-worker",
                        "reasonCode=" + refusal.reasonCode(), clock.instant());
                return rejected;
            }
            AgentDecision.UseTool useTool = (AgentDecision.UseTool) decision;
            PreparedToolCall call = prepareTool(task, useTool, step);
            if (call.effect() == ToolEffect.READ) {
                long startedAt = System.nanoTime();
                ToolObservation observation;
                try {
                    observation = readToolExecutor.execute(call, task);
                } catch (RuntimeException error) {
                    recordToolSafely(
                            call.toolName(), "failed",
                            Duration.ofNanos(System.nanoTime() - startedAt));
                    throw failUnavailable(
                            task, "READ_TOOL_UNAVAILABLE", step, call.toolName(), error);
                }
                recordToolSafely(
                        call.toolName(), "succeeded",
                        Duration.ofNanos(System.nanoTime() - startedAt));
                task = save(task.recordObservation(observation), task.version());
                auditTrail.append(
                        task.taskId(), "READ_TOOL_SUCCEEDED", "ticket-agent-worker",
                        "tool=" + call.toolName() + ", taskVersion=" + task.version(), clock.instant());
                continue;
            }
            long confirmationVersion = task.version() + 1;
            ConfirmationRequest confirmation = new ConfirmationRequest(
                    confirmationIdGenerator.get(),
                    actionIdGenerator.get(),
                    call.toolName(),
                    call.risk(),
                    call.requiredRole(),
                    call.arguments(),
                    call.fingerprint(),
                    confirmationVersion,
                    clock.instant().plus(confirmationTtl));
            AgentTask waiting = save(
                    task.waitForConfirmation(confirmation, clock.instant()), task.version());
            auditTrail.append(
                    task.taskId(), "CONFIRMATION_REQUESTED", "ticket-agent-worker",
                    "tool=" + call.toolName()
                            + ", risk=" + call.risk()
                            + ", taskVersion=" + waiting.version(),
                    clock.instant());
            return waiting;
        }
        AgentTask failed = save(task.fail("STEP_BUDGET_EXCEEDED", clock.instant()), task.version());
        auditTrail.append(
                task.taskId(), "AGENT_TASK_FAILED", "ticket-agent-worker",
                "reasonCode=STEP_BUDGET_EXCEEDED", clock.instant());
        return failed;
    }

    private AgentPlanningResult plan(AgentTask task, int step) {
        try {
            return java.util.Objects.requireNonNull(planner.plan(new AgentPlanningContext(
                            task.taskId(),
                            task.request().objective(),
                            task.request().businessContext(),
                            task.observations(),
                            toolCatalog.toolArgumentNames(),
                            step)),
                    "planner returned no result");
        } catch (RuntimeException error) {
            throw failUnavailable(task, "PLANNER_UNAVAILABLE", step, null, error);
        }
    }

    private PreparedToolCall prepareTool(AgentTask task, AgentDecision.UseTool proposal, int step) {
        try {
            return toolCatalog.prepare(proposal);
        } catch (IllegalArgumentException error) {
            String trustedToolName = toolCatalog.toolNames().contains(proposal.toolName())
                    ? proposal.toolName()
                    : null;
            throw failUnavailable(task, "INVALID_TOOL_PROPOSAL", step, trustedToolName, error);
        }
    }

    private AgentRunUnavailableException failUnavailable(
            AgentTask running,
            String reasonCode,
            int step,
            String toolName,
            RuntimeException cause
    ) {
        AgentTask failed = save(running.fail(reasonCode, clock.instant()), running.version());
        String detail = "reasonCode=" + reasonCode + ", step=" + step;
        if (toolName != null) detail += ", tool=" + toolName;
        if (cause instanceof ReadToolUnavailableException readFailure) {
            detail += ", dependencyFailure=" + readFailure.failureKind();
        }
        try {
            auditTrail.append(
                    failed.taskId(), "AGENT_TASK_FAILED", "ticket-agent-worker",
                    detail, clock.instant());
        } catch (RuntimeException auditFailure) {
            cause.addSuppressed(auditFailure);
        }
        return new AgentRunUnavailableException(reasonCode, cause);
    }

    private static String decisionType(AgentDecision decision) {
        if (decision instanceof AgentDecision.UseTool) return "USE_TOOL";
        if (decision instanceof AgentDecision.Finish) return "FINISH";
        return "REFUSE";
    }

    private void recordPlanSafely(AgentPlanningResult result) {
        try {
            telemetry.recordPlan(result);
        } catch (RuntimeException ignored) {
            // Metrics are diagnostic and must not alter the Agent task state.
        }
    }

    private void recordToolSafely(String toolName, String outcome, Duration duration) {
        try {
            telemetry.recordTool(toolName, outcome, duration);
        } catch (RuntimeException ignored) {
            // Metrics are diagnostic and must not alter the Agent task state.
        }
    }

    private AgentTask save(AgentTask updated, long expectedVersion) {
        return repository.save(updated, expectedVersion);
    }
}
