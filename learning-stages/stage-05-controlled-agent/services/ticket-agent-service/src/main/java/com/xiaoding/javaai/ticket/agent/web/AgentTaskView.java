package com.xiaoding.javaai.ticket.agent.web;

import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.task.AgentTask;

import java.time.Instant;
import java.util.Map;

public record AgentTaskView(
        String taskId,
        String caseId,
        String status,
        long version,
        String outcome,
        ConfirmationView confirmation,
        Instant updatedAt
) {
    static AgentTaskView from(AgentTask task) {
        return new AgentTaskView(
                task.taskId(),
                task.request().caseId(),
                task.state().name(),
                task.version(),
                task.outcome(),
                ConfirmationView.from(task.confirmation()),
                task.updatedAt());
    }

    public record ConfirmationView(
            String confirmationId,
            String actionId,
            String toolName,
            String risk,
            String requiredRole,
            Map<String, String> arguments,
            long boundTaskVersion,
            Instant expiresAt
    ) {
        static ConfirmationView from(ConfirmationRequest request) {
            if (request == null) return null;
            return new ConfirmationView(
                    request.confirmationId(),
                    request.actionId(),
                    request.toolName(),
                    request.risk().name(),
                    request.requiredRole(),
                    request.arguments(),
                    request.boundTaskVersion(),
                    request.expiresAt());
        }
    }
}
