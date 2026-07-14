package com.xiaoding.javaai.eval.agent;

import java.util.List;

public record AgentEvaluationSnapshot(
        String taskId,
        String state,
        String toolName,
        String risk,
        String requiredRole,
        List<String> auditEventTypes,
        List<String> auditDetails,
        long latencyMillis
) {
    public AgentEvaluationSnapshot {
        auditEventTypes = List.copyOf(auditEventTypes);
        auditDetails = List.copyOf(auditDetails);
        if (latencyMillis < 0) throw new IllegalArgumentException("latencyMillis must not be negative");
    }

    public AgentEvaluationSnapshot(
            String taskId,
            String state,
            String toolName,
            String risk,
            String requiredRole,
            List<String> auditEventTypes,
            long latencyMillis
    ) {
        this(taskId, state, toolName, risk, requiredRole, auditEventTypes, List.of(), latencyMillis);
    }
}
