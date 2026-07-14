package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.Toolkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class EnterpriseMcpRegistry {

    private final Toolkit toolkit;
    private final Set<String> allowedServers;
    private final Set<String> allowedTools;

    public EnterpriseMcpRegistry(Toolkit toolkit, Set<String> allowedServers, Set<String> allowedTools) {
        this.toolkit = Objects.requireNonNull(toolkit, "toolkit must not be null");
        this.allowedServers = Set.copyOf(allowedServers);
        this.allowedTools = Set.copyOf(allowedTools);
    }

    public McpRegistrationReceipt register(McpServerDescriptor server, List<McpToolDescriptor> tools) {
        validateServer(server);
        List<String> registered = new ArrayList<>();
        for (McpToolDescriptor tool : tools) {
            validateTool(tool);
            toolkit.registerSchema(ToolSchema.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .parameters(tool.inputSchema())
                    .strict(true)
                    .build());
            registered.add(tool.name());
        }
        return new McpRegistrationReceipt(server.serverId(), server.endpoint(), registered);
    }

    private void validateServer(McpServerDescriptor server) {
        if (server == null || !allowedServers.contains(server.serverId())) {
            throw new IllegalArgumentException("MCP server is not allowlisted");
        }
        if (server.endpoint() == null || !"https".equalsIgnoreCase(server.endpoint().getScheme())
                || server.endpoint().getHost() == null) {
            throw new IllegalArgumentException("MCP endpoint must use HTTPS with a host");
        }
    }

    private void validateTool(McpToolDescriptor tool) {
        if (tool == null || !allowedTools.contains(tool.name())) {
            throw new IllegalArgumentException("MCP tool is not allowlisted");
        }
        if (!tool.readOnly()) {
            throw new IllegalArgumentException("write MCP tools require a separate confirmation policy");
        }
        if (tool.description() == null || tool.description().isBlank()
                || !"object".equals(tool.inputSchema().get("type"))) {
            throw new IllegalArgumentException("MCP tool schema is incomplete");
        }
    }
}
