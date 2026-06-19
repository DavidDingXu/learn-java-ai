package cn.dingxu.javaai.tool.service;

import java.time.Instant;
import java.util.Map;

public record ToolExecutionRecord(
        String traceId,
        String toolName,
        String userId,
        String tenantId,
        boolean approved,
        boolean success,
        Map<String, Object> arguments,
        String message,
        long durationMs,
        Instant executedAt
) {
    public ToolExecutionRecord {
        traceId = traceId == null || traceId.isBlank() ? "trace-local" : traceId;
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        durationMs = Math.max(durationMs, 0);
        executedAt = executedAt == null ? Instant.now() : executedAt;
    }
}
