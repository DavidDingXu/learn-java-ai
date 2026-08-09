package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.LegacyWriteToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.ToolExecutionRejectedException;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.task.AgentTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

final class InMemoryLegacyWriteToolExecutor implements LegacyWriteToolExecutor {

    private final ConcurrentMap<String, StoredExecution> executions = new ConcurrentHashMap<>();
    private final AtomicLong auditSequence = new AtomicLong();

    @Override
    public ToolExecutionReceipt execute(
            AgentTask task,
            ConfirmationRequest confirmation,
            String idempotencyKey
    ) {
        java.util.Objects.requireNonNull(task, "task must not be null");
        java.util.Objects.requireNonNull(confirmation, "confirmation must not be null");
        String normalizedKey = requireText(idempotencyKey, "idempotencyKey");
        StoredExecution candidate = new StoredExecution(
                confirmation.actionId(), "local-audit-" + auditSequence.incrementAndGet());
        StoredExecution stored = executions.putIfAbsent(normalizedKey, candidate);
        if (stored == null) {
            return candidate.receipt(false);
        }
        if (!stored.actionId().equals(confirmation.actionId())) {
            throw new ToolExecutionRejectedException(
                    "IDEMPOTENCY_CONFLICT",
                    "idempotency key is already bound to another action");
        }
        return stored.receipt(true);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }

    private record StoredExecution(String actionId, String auditId) {
        ToolExecutionReceipt receipt(boolean duplicate) {
            return new ToolExecutionReceipt(actionId, "SUCCEEDED", duplicate, auditId);
        }
    }
}
