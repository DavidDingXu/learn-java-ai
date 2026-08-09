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
        String fingerprint = decisionFingerprint(taskId, command);
        ConfirmationDecisionStore.StoredDecision stored = decisions.executeOnce(
                principalScope(actor),
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
        recordApprovalBeforeRemoteCall(executing, actor, command);
        String toolIdempotencyKey = toolIdempotencyKey(
                waiting.identity().tenantId(), confirmation.actionId());
        long startedAt = System.nanoTime();
        ToolExecutionReceipt toolReceipt;
        try {
            toolReceipt = executor.execute(executing, confirmation, toolIdempotencyKey);
        } catch (ToolExecutionRejectedException error) {
            AgentTask failed = tasks.save(
                    executing.executionFailed(error.getMessage(), clock.instant()), executing.version());
            recordToolSafely(
                    confirmation.toolName(), "rejected",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            AgentAuditEvent event = auditTrail.append(
                    taskId, "TOOL_EXECUTION_REJECTED", actor.subjectId(),
                    error.getMessage(), clock.instant());
            return new ConfirmationDecisionReceipt(
                    taskId, failed.state(), confirmation.actionId(), "REJECTED",
                    auditId(event), failed.version(), false);
        } catch (RemoteExecutionUncertainException error) {
            AgentTask uncertain = tasks.save(
                    executing.executionUncertain(error.getMessage(), clock.instant()), executing.version());
            recordToolSafely(
                    confirmation.toolName(), "uncertain",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            AgentAuditEvent event = auditTrail.append(
                    taskId, "TOOL_EXECUTION_UNCERTAIN", actor.subjectId(),
                    error.getMessage(), clock.instant());
            return new ConfirmationDecisionReceipt(
                    taskId, uncertain.state(), confirmation.actionId(), "UNKNOWN",
                    auditId(event), uncertain.version(), false);
        } catch (RuntimeException error) {
            AgentTask failed = tasks.save(
                    executing.executionFailed(error.getMessage(), clock.instant()), executing.version());
            recordToolSafely(
                    confirmation.toolName(), "failed",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            auditTrail.append(
                    taskId, "TOOL_EXECUTION_FAILED", actor.subjectId(),
                    error.getClass().getSimpleName(), clock.instant());
            if (failed.state() != AgentTaskState.FAILED) {
                throw new IllegalStateException("agent task did not enter FAILED", error);
            }
            throw error;
        }
        return completeRemoteSuccess(executing, confirmation, actor, toolReceipt, startedAt);
    }

    private void recordApprovalBeforeRemoteCall(
            AgentTask executing,
            ConfirmationActor actor,
            ConfirmToolAction command
    ) {
        try {
            auditTrail.append(
                    executing.taskId(), "CONFIRMATION_APPROVED", actor.subjectId(),
                    command.reason(), clock.instant());
        } catch (RuntimeException auditFailure) {
            try {
                tasks.save(
                        executing.preExecutionFailed("APPROVAL_AUDIT_UNAVAILABLE", clock.instant()),
                        executing.version());
            } catch (RuntimeException stateFailure) {
                auditFailure.addSuppressed(stateFailure);
            }
            throw auditFailure;
        }
    }

    private ConfirmationDecisionReceipt completeRemoteSuccess(
            AgentTask executing,
            ConfirmationRequest confirmation,
            ConfirmationActor actor,
            ToolExecutionReceipt toolReceipt,
            long startedAt
    ) {
        try {
            auditTrail.append(
                    executing.taskId(), "TOOL_EXECUTION_SUCCEEDED", actor.subjectId(),
                    "actionId=" + toolReceipt.actionId() + ", legacyAuditId=" + toolReceipt.auditId(),
                    clock.instant());
            AgentTask completed = tasks.save(
                    executing.executionSucceeded(toolReceipt, clock.instant()), executing.version());
            recordToolSafely(
                    confirmation.toolName(), "succeeded",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt));
            return new ConfirmationDecisionReceipt(
                    executing.taskId(), completed.state(), confirmation.actionId(), toolReceipt.status(),
                    toolReceipt.auditId(), completed.version(), false);
        } catch (RuntimeException localFailure) {
            return markLocalCompletionUncertain(
                    executing, confirmation, actor, toolReceipt, startedAt, localFailure);
        }
    }

    private ConfirmationDecisionReceipt markLocalCompletionUncertain(
            AgentTask executing,
            ConfirmationRequest confirmation,
            ConfirmationActor actor,
            ToolExecutionReceipt toolReceipt,
            long startedAt,
            RuntimeException localFailure
    ) {
        AgentTask current = tasks.findById(executing.taskId())
                .orElseThrow(() -> new AgentTaskNotFoundException(executing.taskId()));
        if (current.state() == AgentTaskState.COMPLETED) {
            return new ConfirmationDecisionReceipt(
                    current.taskId(), current.state(), confirmation.actionId(), toolReceipt.status(),
                    toolReceipt.auditId(), current.version(), false);
        }
        if (current.state() == AgentTaskState.EXECUTING) {
            current = tasks.save(
                    current.executionUncertain(
                            "LOCAL_COMPLETION_FAILED", clock.instant()),
                    current.version());
        } else if (current.state() != AgentTaskState.EXECUTION_UNCERTAIN) {
            throw localFailure;
        }

        AgentAuditEvent event = null;
        try {
            event = auditTrail.append(
                    current.taskId(), "TOOL_EXECUTION_UNCERTAIN", actor.subjectId(),
                    "reasonCode=LOCAL_COMPLETION_FAILED, actionId=" + confirmation.actionId(),
                    clock.instant());
        } catch (RuntimeException auditFailure) {
            localFailure.addSuppressed(auditFailure);
        }
        recordToolSafely(
                confirmation.toolName(), "uncertain",
                java.time.Duration.ofNanos(System.nanoTime() - startedAt));
        return new ConfirmationDecisionReceipt(
                current.taskId(), current.state(), confirmation.actionId(), "UNKNOWN",
                event == null ? current.taskId() + ":audit-unavailable" : auditId(event),
                current.version(), false);
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
        if (!ToolActionFingerprint.matches(
                confirmation.actionFingerprint(), confirmation.toolName(), confirmation.arguments())) {
            throw new IllegalStateException("confirmation action fingerprint does not match its action snapshot");
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

    static String decisionFingerprint(String taskId, ConfirmToolAction command) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, "taskId", taskId);
        append(canonical, "confirmationId", command.confirmationId());
        append(canonical, "expectedTaskVersion", Long.toString(command.expectedTaskVersion()));
        append(canonical, "decision", command.decision().name());
        append(canonical, "reason", command.reason());
        return sha256(canonical);
    }

    static String principalScope(ConfirmationActor actor) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, "tenantId", actor.tenantId());
        append(canonical, "actorId", actor.actorId());
        append(canonical, "subjectId", actor.subjectId());
        return "sha256:" + sha256(canonical);
    }

    static String toolIdempotencyKey(String tenantId, String actionId) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, "tenantId", tenantId);
        append(canonical, "actionId", actionId);
        return "tool:v1:" + sha256(canonical);
    }

    private static String sha256(CharSequence canonical) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static void append(StringBuilder canonical, String name, String value) {
        appendValue(canonical, name);
        appendValue(canonical, java.util.Objects.requireNonNull(value, name + " must not be null"));
    }

    private static void appendValue(StringBuilder canonical, String value) {
        canonical.append(value.length()).append(':').append(value).append(';');
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

    private void recordToolSafely(String toolName, String outcome, java.time.Duration duration) {
        try {
            telemetry.recordTool(toolName, outcome, duration);
        } catch (RuntimeException ignored) {
            // Metrics are diagnostic; they must not change an already persisted business result.
        }
    }

    private static String auditId(AgentAuditEvent event) {
        return event.taskId() + ":" + event.sequence();
    }
}
