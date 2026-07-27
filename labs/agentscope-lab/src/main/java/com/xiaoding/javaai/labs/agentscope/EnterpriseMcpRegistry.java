package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.tool.Toolkit;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class EnterpriseMcpRegistry {

    private final Toolkit toolkit;
    private final Map<String, URI> managedServers;
    private final Set<String> allowedTools;

    public EnterpriseMcpRegistry(
            Toolkit toolkit,
            Map<String, URI> managedServers,
            Set<String> allowedTools
    ) {
        this.toolkit = Objects.requireNonNull(toolkit, "toolkit must not be null");
        this.managedServers = Map.copyOf(managedServers);
        this.managedServers.forEach(EnterpriseMcpRegistry::validateManagedEndpoint);
        this.allowedTools = Set.copyOf(allowedTools);
    }

    public McpRegistrationReceipt register(McpServerDescriptor server, List<McpToolDescriptor> tools) {
        URI managedEndpoint = resolveManagedEndpoint(server);
        List<McpToolDescriptor> discoveredTools = List.copyOf(
                Objects.requireNonNull(tools, "tools must not be null"));
        discoveredTools.forEach(this::validateTool);

        List<String> registered = new ArrayList<>();
        for (McpToolDescriptor tool : discoveredTools) {
            toolkit.registerSchema(ToolSchema.builder()
                    .name(tool.name())
                    .description(tool.description())
                    .parameters(tool.inputSchema())
                    .strict(true)
                    .build());
            registered.add(tool.name());
        }
        return new McpRegistrationReceipt(server.serverId(), managedEndpoint, registered);
    }

    private URI resolveManagedEndpoint(McpServerDescriptor server) {
        URI endpoint = server == null ? null : managedServers.get(server.serverId());
        if (endpoint == null) {
            throw new IllegalArgumentException("MCP server is not allowlisted");
        }
        return endpoint;
    }

    private static void validateManagedEndpoint(String serverId, URI endpoint) {
        if (serverId == null || serverId.isBlank() || endpoint == null
                || !"https".equalsIgnoreCase(endpoint.getScheme())
                || endpoint.getHost() == null
                || endpoint.getUserInfo() != null
                || endpoint.getQuery() != null
                || endpoint.getFragment() != null) {
            throw new IllegalArgumentException(
                    "managed MCP endpoints must use HTTPS without user info, query or fragment");
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
