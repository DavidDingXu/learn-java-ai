package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;

public record AgentPlanningResult(
        AgentDecision decision,
        String model,
        AgentModelUsage usage,
        String finishReason
) {
    public AgentPlanningResult {
        decision = java.util.Objects.requireNonNull(decision, "decision must not be null");
        model = normalize(model, "unknown");
        usage = usage == null ? AgentModelUsage.unknown() : usage;
        finishReason = normalize(finishReason, "unknown");
    }

    public static AgentPlanningResult decisionOnly(AgentDecision decision) {
        return new AgentPlanningResult(decision, "test-double", AgentModelUsage.unknown(), "not-applicable");
    }

    private static String normalize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
