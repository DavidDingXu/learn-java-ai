package com.xiaoding.javaai.mcp;

import java.time.Instant;
import java.util.List;

public record McpClientSession(
        String hostName,
        String serverName,
        List<String> tools,
        List<String> resources,
        List<String> prompts,
        Instant initializedAt
) {
    public McpClientSession {
        if (hostName == null || hostName.isBlank()) {
            throw new IllegalArgumentException("hostName must not be blank");
        }
        if (serverName == null || serverName.isBlank()) {
            throw new IllegalArgumentException("serverName must not be blank");
        }
        tools = tools == null ? List.of() : List.copyOf(tools);
        resources = resources == null ? List.of() : List.copyOf(resources);
        prompts = prompts == null ? List.of() : List.copyOf(prompts);
        initializedAt = initializedAt == null ? Instant.now() : initializedAt;
    }
}
