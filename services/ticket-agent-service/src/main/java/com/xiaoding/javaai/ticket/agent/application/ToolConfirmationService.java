package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationDecision;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskAccessDeniedException;
import com.xiaoding.javaai.ticket.task.AgentTaskNotFoundException;
import com.xiaoding.javaai.ticket.task.AgentTaskRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

public final class ToolConfirmationService implements ConfirmAgentAction {

    private final AgentTaskRepository tasks;
    private final ConfirmationDecisionStore decisions;
    private final LegacyWriteToolExecutor executor;
    private final AgentAuditTrail auditTrail;
    private final Clock clock;
    private final AgentTelemetry telemetry;

    public ToolConfirmationService(
            AgentTaskRepository tasks,
            ConfirmationDecisionStore decisions,
            LegacyWriteToolExecutor executor,
            AgentAuditTrail auditTrail,
            Clock clock
    ) {
        this(tasks, decisions, executor, auditTrail, clock, AgentTelemetry.NOOP);
    }

    public ToolConfirmationService(
            AgentTaskRepository tasks,
            ConfirmationDecisionStore decisions,
            LegacyWriteToolExecutor executor,
            AgentAuditTrail auditTrail,
            Clock clock,
            AgentTelemetry telemetry
    ) {
        this.tasks = tasks;
        this.decisions = decisions;
        this.executor = executor;
        this.auditTrail = auditTrail;
        this.clock = clock;
        this.telemetry = java.util.Objects.requireNonNull(telemetry, "telemetry must not be null");
    }

    @Override
    public ConfirmationDecisionReceipt decide(
            String taskId,
            ConfirmationActor actor,
            String idempotencyKey,
            ConfirmToolAction command
    ) {
        String fingerprint = fingerprint(taskId, command);
        ConfirmationDecisionStore.StoredDecision stored = decisions.executeOnce(
                String.join("\n", actor.tenantId(), actor.actorId(), actor.subjectId()),
                requireKey(idempotencyKey), fingerprint,
                () -> executeDecision(taskId, actor, command));
        return stored.duplicate() ? stored.receipt().asDuplicate() : stored.receipt();
    }

    private ConfirmationDecisionReceipt executeDecision(
            String taskId,
            ConfirmationActor actor,
            ConfirmToolAction command
    ) {
        AgentTask waiting = tasks.findById(taskId)
                .orElseThrow(() -> new AgentTaskNotFoundException(taskId));
        validate(waiting, actor, command);
        ConfirmationRequest confirmation = waiting.confirmation();
        if (command.decision() == ConfirmationDecision.REJECT) {
            AgentTask rejected = tasks.save(
                    waiting.rejectConfirmation(command.reason(), clock.instant()), waiting.version());
            AgentAuditEvent event = auditTrail.append(
                    taskId, "CONFIRMATION_REJECTED", actor.subjectId(), command.reason(), clock.instant());
            return new ConfirmationDecisionReceipt(
                    taskId, rejected.state(), confirmation.actionId(), "NOT_EXECUTED",
                    auditId(event), rejected.version(), false);
        }

        AgentTask executing = tasks.save(
                waiting.beginExecution(clock.instant()), waiting.version());
        auditTrail.append(
                taskId, "CONFIRMATION_APPROVED", actor.subjectId(), command.reason(), clock.instant());
        String toolIdempotencyKey = "tool:" + waiting.identity().tenantId() + ":" + confirmation.actionId();
        long startedAt = System.nanoTime();
        try {
            ToolExecutionReceipt toolReceipt = executor.execute(
                    executing, confirmation, toolIdempotencyKey);
            AgentTask completed = tasks.save(
                    executing.executionSucceeded(toolReceipt, clock.instant()), executing.version());
            auditTrail.append(
                    taskId, "TOOL_EXECUTION_SUCCEEDED", actor.subjectId(),
                    "actionId=" + toolReceipt.actionId() + ", legacyAuditId=" + toolReceipt.auditId(),
                    clock.instant());
            telemetry.recordTool(
                    confirmation.toolName(), "succeeded",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            return new ConfirmationDecisionReceipt(
                    taskId, completed.state(), confirmation.actionId(), toolReceipt.status(),
                    toolReceipt.auditId(), completed.version(), false);
        } catch (ToolExecutionRejectedException error) {
            telemetry.recordTool(
                    confirmation.toolName(), "rejected",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            AgentTask failed = tasks.save(
                    executing.executionFailed(error.getMessage(), clock.instant()), executing.version());
            AgentAuditEvent event = auditTrail.append(
                    taskId, "TOOL_EXECUTION_REJECTED", actor.subjectId(),
                    error.getMessage(), clock.instant());
            return new ConfirmationDecisionReceipt(
                    taskId, failed.state(), confirmation.actionId(), "REJECTED",
                    auditId(event), failed.version(), false);
        } catch (RemoteExecutionUncertainException error) {
            telemetry.recordTool(
                    confirmation.toolName(), "uncertain",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            AgentTask uncertain = tasks.save(
                    executing.executionUncertain(error.getMessage(), clock.instant()), executing.version());
            AgentAuditEvent event = auditTrail.append(
                    taskId, "TOOL_EXECUTION_UNCERTAIN", actor.subjectId(),
                    error.getMessage(), clock.instant());
            return new ConfirmationDecisionReceipt(
                    taskId, uncertain.state(), confirmation.actionId(), "UNKNOWN",
                    auditId(event), uncertain.version(), false);
        } catch (RuntimeException error) {
            telemetry.recordTool(
                    confirmation.toolName(), "failed",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            AgentTask failed = tasks.save(
                    executing.executionFailed(error.getMessage(), clock.instant()), executing.version());
            auditTrail.append(
                    taskId, "TOOL_EXECUTION_FAILED", actor.subjectId(),
                    error.getClass().getSimpleName(), clock.instant());
            if (failed.state() != AgentTaskState.FAILED) {
                throw new IllegalStateException("agent task did not enter FAILED", error);
            }
            throw error;
        }
    }

    private void validate(AgentTask task, ConfirmationActor actor, ConfirmToolAction command) {
        if (!task.identity().tenantId().equals(actor.tenantId())) {
            throw new AgentTaskAccessDeniedException("confirmation actor belongs to a different tenant");
        }
        if (!"jdk8-crm".equals(actor.actorId())) {
            throw new AgentTaskAccessDeniedException("confirmation actor must come from jdk8-crm");
        }
        if (task.state() != AgentTaskState.WAITING_CONFIRMATION) {
            throw new IllegalStateException("agent task is not waiting for confirmation");
        }
        ConfirmationRequest confirmation = task.confirmation();
        if (!confirmation.confirmationId().equals(command.confirmationId())) {
            throw new IllegalArgumentException("confirmationId does not match the pending action");
        }
        if (task.version() != command.expectedTaskVersion()
                || confirmation.boundTaskVersion() != command.expectedTaskVersion()) {
            throw new IllegalStateException("confirmation task version is stale");
        }
        if (!clock.instant().isBefore(confirmation.expiresAt())) {
            throw new IllegalStateException("confirmation has expired");
        }
        if (!actor.roles().contains(confirmation.requiredRole())) {
            throw new AgentTaskAccessDeniedException(
                    "confirmation actor lacks required role: " + confirmation.requiredRole());
        }
    }

    private static String fingerprint(String taskId, ConfirmToolAction command) {
        String canonical = taskId + '\n'
                + command.confirmationId() + '\n'
                + command.expectedTaskVersion() + '\n'
                + command.decision() + '\n'
                + command.reason();
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static String requireKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() < 8 || normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key length must be between 8 and 128");
        }
        return normalized;
    }

    private static String auditId(AgentAuditEvent event) {
        return event.taskId() + ":" + event.sequence();
    }
}
