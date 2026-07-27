package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AgentTask {

    private final String taskId;
    private final DelegatedTicketIdentity identity;
    private final AgentTaskRequest request;
    private final Instant createdAt;
    private final AgentTaskState state;
    private final long version;
    private final List<ToolObservation> observations;
    private final ConfirmationRequest confirmation;
    private final String outcome;
    private final Instant updatedAt;

    private AgentTask(
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
        this.taskId = requireText(taskId, "taskId");
        this.identity = java.util.Objects.requireNonNull(identity, "identity must not be null");
        this.request = java.util.Objects.requireNonNull(request, "request must not be null");
        this.createdAt = java.util.Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.state = java.util.Objects.requireNonNull(state, "state must not be null");
        this.version = version;
        this.observations = List.copyOf(observations);
        this.confirmation = confirmation;
        this.outcome = outcome;
        this.updatedAt = java.util.Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static AgentTask accepted(
            String taskId,
            DelegatedTicketIdentity identity,
            AgentTaskRequest request,
            Instant createdAt
    ) {
        return new AgentTask(
                taskId, identity, request, createdAt,
                AgentTaskState.ACCEPTED, 0, List.of(), null, null, createdAt);
    }

    public static AgentTask restore(
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
        return new AgentTask(
                taskId, identity, request, createdAt, state, version,
                observations, confirmation, outcome, updatedAt);
    }

    public String taskId() {
        return taskId;
    }

    public DelegatedTicketIdentity identity() {
        return identity;
    }

    public AgentTaskRequest request() {
        return request;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String status() {
        return state.name();
    }

    public AgentTaskState state() {
        return state;
    }

    public long version() {
        return version;
    }

    public List<ToolObservation> observations() {
        return observations;
    }

    public ConfirmationRequest confirmation() {
        return confirmation;
    }

    public String outcome() {
        return outcome;
    }

    public Instant updatedAt() {
        return updatedAt;
    }

    public AgentTask start(Instant now) {
        requireState(AgentTaskState.ACCEPTED);
        return transition(AgentTaskState.RUNNING, observations, null, null, now);
    }

    public AgentTask recordObservation(ToolObservation observation) {
        requireState(AgentTaskState.RUNNING);
        ArrayList<ToolObservation> updated = new ArrayList<>(observations);
        updated.add(java.util.Objects.requireNonNull(observation, "observation must not be null"));
        return transition(AgentTaskState.RUNNING, updated, null, null, observation.observedAt());
    }

    public AgentTask waitForConfirmation(ConfirmationRequest request, Instant now) {
        requireState(AgentTaskState.RUNNING);
        if (request.boundTaskVersion() != version + 1) {
            throw new IllegalArgumentException("confirmation must bind the resulting task version");
        }
        return transition(AgentTaskState.WAITING_CONFIRMATION, observations, request, null, now);
    }

    public AgentTask complete(String summary, Instant now) {
        requireState(AgentTaskState.RUNNING);
        return transition(AgentTaskState.COMPLETED, observations, null,
                requireText(summary, "summary"), now);
    }

    public AgentTask reject(String reason, Instant now) {
        requireState(AgentTaskState.RUNNING);
        return transition(AgentTaskState.REJECTED, observations, null,
                requireText(reason, "reason"), now);
    }

    public AgentTask fail(String reason, Instant now) {
        requireState(AgentTaskState.RUNNING);
        return transition(AgentTaskState.FAILED, observations, null,
                requireText(reason, "reason"), now);
    }

    public AgentTask beginExecution(Instant now) {
        requireState(AgentTaskState.WAITING_CONFIRMATION);
        return transition(AgentTaskState.EXECUTING, observations, confirmation, null, now);
    }

    public AgentTask executionSucceeded(ToolExecutionReceipt receipt, Instant now) {
        requireState(AgentTaskState.EXECUTING);
        java.util.Objects.requireNonNull(receipt, "receipt must not be null");
        if (!confirmation.actionId().equals(receipt.actionId())) {
            throw new IllegalArgumentException("tool receipt actionId does not match confirmation");
        }
        return transition(
                AgentTaskState.COMPLETED,
                observations,
                confirmation,
                "TOOL_SUCCEEDED: " + receipt.status() + ", auditId=" + receipt.auditId(),
                now);
    }

    public AgentTask executionUncertain(String reason, Instant now) {
        requireState(AgentTaskState.EXECUTING);
        return transition(
                AgentTaskState.EXECUTION_UNCERTAIN,
                observations,
                confirmation,
                "TOOL_EXECUTION_UNCERTAIN: " + requireText(reason, "reason"),
                now);
    }

    public AgentTask executionFailed(String reason, Instant now) {
        requireState(AgentTaskState.EXECUTING);
        return transition(
                AgentTaskState.FAILED,
                observations,
                confirmation,
                "TOOL_EXECUTION_FAILED: " + requireText(reason, "reason"),
                now);
    }

    public AgentTask preExecutionFailed(String reason, Instant now) {
        requireState(AgentTaskState.EXECUTING);
        return transition(
                AgentTaskState.FAILED,
                observations,
                confirmation,
                "LOCAL_PRE_EXECUTION_FAILED: " + requireText(reason, "reason"),
                now);
    }

    public AgentTask rejectConfirmation(String reason, Instant now) {
        requireState(AgentTaskState.WAITING_CONFIRMATION);
        return transition(
                AgentTaskState.REJECTED,
                observations,
                confirmation,
                "CONFIRMATION_REJECTED: " + requireText(reason, "reason"),
                now);
    }

    private AgentTask transition(
            AgentTaskState target,
            List<ToolObservation> nextObservations,
            ConfirmationRequest nextConfirmation,
            String nextOutcome,
            Instant now
    ) {
        return new AgentTask(
                taskId, identity, request, createdAt,
                target, version + 1, nextObservations, nextConfirmation, nextOutcome, now);
    }

    private void requireState(AgentTaskState expected) {
        if (state != expected) {
            throw new IllegalStateException("agent task must be " + expected + ", but was " + state);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
