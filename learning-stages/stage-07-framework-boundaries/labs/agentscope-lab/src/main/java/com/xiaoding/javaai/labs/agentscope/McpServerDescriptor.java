package com.xiaoding.javaai.labs.agentscope;

public record McpServerDescriptor(String serverId) {

    public McpServerDescriptor {
        if (serverId == null || serverId.isBlank()) {
            throw new IllegalArgumentException("MCP server ID must not be blank");
        }
        serverId = serverId.strip();
    }
}
