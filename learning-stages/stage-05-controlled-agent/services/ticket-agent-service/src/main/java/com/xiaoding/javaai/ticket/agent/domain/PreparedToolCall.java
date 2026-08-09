package com.xiaoding.javaai.ticket.agent.domain;

import java.util.Map;

public record PreparedToolCall(
        String toolName,
        ToolEffect effect,
        ToolRisk risk,
        String requiredRole,
        Map<String, String> arguments,
        String rationale,
        String fingerprint
) {
    public PreparedToolCall {
        toolName = requireText(toolName, "toolName");
        effect = java.util.Objects.requireNonNull(effect, "effect must not be null");
        risk = java.util.Objects.requireNonNull(risk, "risk must not be null");
        requiredRole = requiredRole == null ? "" : requiredRole.trim();
        arguments = Map.copyOf(arguments);
        rationale = requireText(rationale, "rationale");
        fingerprint = requireText(fingerprint, "fingerprint");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
