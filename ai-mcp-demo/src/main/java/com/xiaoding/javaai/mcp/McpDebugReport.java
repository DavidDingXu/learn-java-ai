package com.xiaoding.javaai.mcp;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record McpDebugReport(
        String endpoint,
        McpTransportType transportType,
        Duration connectTimeout,
        boolean connected,
        List<String> tools,
        List<String> resources,
        List<String> prompts,
        String error,
        Instant checkedAt
) {
    public McpDebugReport {
        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("endpoint must not be blank");
        }
        transportType = transportType == null ? McpTransportType.STREAMABLE_HTTP : transportType;
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        tools = tools == null ? List.of() : List.copyOf(tools);
        resources = resources == null ? List.of() : List.copyOf(resources);
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
        error = error == null ? "" : error;
        checkedAt = checkedAt == null ? Instant.now() : checkedAt;
    }
}
