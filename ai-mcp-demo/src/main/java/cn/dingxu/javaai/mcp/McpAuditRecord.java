package cn.dingxu.javaai.mcp;

import java.time.Instant;
import java.util.Map;

public record McpAuditRecord(
        String toolName,
        String userId,
        String tenantId,
        boolean allowed,
        boolean success,
        String message,
        String denyReason,
        Map<String, Object> arguments,
        Instant executedAt
) {
    public McpAuditRecord {
        denyReason = denyReason == null ? "" : denyReason;
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
        executedAt = executedAt == null ? Instant.now() : executedAt;
    }
}
