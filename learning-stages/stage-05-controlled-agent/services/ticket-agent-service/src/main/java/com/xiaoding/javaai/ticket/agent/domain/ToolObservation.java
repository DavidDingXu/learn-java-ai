package com.xiaoding.javaai.ticket.agent.domain;

import java.time.Instant;
import java.util.Map;

public record ToolObservation(
        String toolName,
        Map<String, String> result,
        Instant observedAt
) {
    public ToolObservation {
        toolName = requireText(toolName, "toolName");
        result = result == null ? Map.of() : Map.copyOf(result);
        observedAt = java.util.Objects.requireNonNull(observedAt, "observedAt must not be null");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
