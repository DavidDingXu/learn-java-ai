package com.xiaoding.javaai.eval.agent;

import java.util.List;
import java.util.Map;

public record AgentEvaluationSnapshot(
        String taskId,
        String state,
        String toolName,
        String risk,
        String requiredRole,
        Map<String, String> arguments,
        List<String> auditEventTypes,
        List<String> auditDetails,
        long latencyMillis
) {
    public AgentEvaluationSnapshot {
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
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
        this(taskId, state, toolName, risk, requiredRole, Map.of(),
                auditEventTypes, List.of(), latencyMillis);
    }
}
