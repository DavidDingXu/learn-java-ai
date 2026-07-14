package com.xiaoding.javaai.labs.protocol.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class EnterpriseMcpClient {

    private final McpSyncClient client;
    private final Set<String> allowedTools;
    private final Map<String, McpSchema.Tool> discoveredTools = new LinkedHashMap<>();

    public EnterpriseMcpClient(McpSyncClient client, Set<String> allowedTools) {
        this.client = java.util.Objects.requireNonNull(client, "client must not be null");
        this.allowedTools = Set.copyOf(allowedTools);
    }

    public McpDiscoveryReceipt initializeAndDiscover() {
        McpSchema.InitializeResult initialized = client.initialize();
        discoveredTools.clear();
        for (McpSchema.Tool tool : client.listTools().tools()) {
            if (!allowedTools.contains(tool.name())) continue;
            validateReadTool(tool);
            discoveredTools.put(tool.name(), tool);
        }
        if (discoveredTools.size() != allowedTools.size()) {
            Set<String> missing = new java.util.LinkedHashSet<>(allowedTools);
            missing.removeAll(discoveredTools.keySet());
            throw new IllegalStateException("allowlisted MCP tools were not discovered: " + missing);
        }
        return new McpDiscoveryReceipt(
                initialized.protocolVersion(),
                initialized.serverInfo().name(),
                initialized.serverInfo().version(),
                discoveredTools.keySet().stream().toList());
    }

    public McpSchema.CallToolResult callReadTool(String toolName, Map<String, Object> arguments) {
        if (!discoveredTools.containsKey(toolName)) {
            throw new IllegalArgumentException("MCP tool is not registered: " + toolName);
        }
        return client.callTool(McpSchema.CallToolRequest.builder(toolName)
                .arguments(Map.copyOf(arguments))
                .build());
    }

    private static void validateReadTool(McpSchema.Tool tool) {
        McpSchema.ToolAnnotations annotations = tool.annotations();
        if (annotations == null || !Boolean.TRUE.equals(annotations.readOnlyHint())
                || Boolean.TRUE.equals(annotations.destructiveHint())) {
            throw new IllegalArgumentException(
                    "MCP tool requires a locally approved read-only contract: " + tool.name());
        }
        if (!"object".equals(tool.inputSchema().get("type"))) {
            throw new IllegalArgumentException("MCP tool input schema must be an object: " + tool.name());
        }
        if (tool.description() == null || tool.description().isBlank()) {
            throw new IllegalArgumentException("MCP tool description must not be blank: " + tool.name());
        }
    }
}
