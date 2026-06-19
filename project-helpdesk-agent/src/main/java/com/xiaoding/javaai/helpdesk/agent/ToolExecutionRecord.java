package com.xiaoding.javaai.helpdesk.agent;

import java.time.Instant;
import java.util.Map;

public record ToolExecutionRecord(
        String toolName,
        String userId,
        String tenantId,
        boolean approved,
        boolean success,
        Map<String, Object> arguments,
        String message,
        Instant executedAt
) {
    public ToolExecutionRecord {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        message = message == null ? "" : message;
        executedAt = executedAt == null ? Instant.now() : executedAt;
    }
}
