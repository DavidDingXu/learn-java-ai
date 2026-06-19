package com.xiaoding.javaai.mcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class McpHostClient {

    private final PolicyMcpServer server;
    private final ToolAccessPolicy accessPolicy;
    private final McpAuditLedger ledger;

    public McpHostClient(PolicyMcpServer server, Set<String> toolAllowlist, McpAuditLedger ledger) {
        this(server, ToolAccessPolicy.allowlist(toolAllowlist), ledger);
    }

    public McpHostClient(PolicyMcpServer server, ToolAccessPolicy accessPolicy, McpAuditLedger ledger) {
        this.server = server;
        this.accessPolicy = accessPolicy == null ? ToolAccessPolicy.allowlist(Set.of()) : accessPolicy;
        this.ledger = ledger;
    }

    public McpClientSession initialize(String hostName) {
        McpServerManifest manifest = server.manifest();
        return new McpClientSession(
                hostName,
                manifest.serverName(),
                namesOfTools(manifest),
                urisOfResources(manifest),
                namesOfPrompts(manifest),
                Instant.now()
        );
    }

    public McpToolResult callTool(String toolName, Map<String, Object> arguments, OperatorContext operator) {
        ToolAccessDecision decision = accessPolicy.evaluate(toolName, operator);
        if (!decision.allowed()) {
            McpToolResult result = McpToolResult.failed(decision.reason(), Map.of());
            ledger.append(toolName, operator, false, result, arguments, decision.reason());
            return result;
        }

        McpToolResult result = server.callTool(toolName, arguments, operator);
        ledger.append(toolName, operator, true, result, arguments);
        return result;
    }

    private List<String> namesOfTools(McpServerManifest manifest) {
        return manifest.tools().stream()
                .map(McpToolDescriptor::name)
                .toList();
    }

    private List<String> urisOfResources(McpServerManifest manifest) {
        return manifest.resources().stream()
                .map(McpResourceDescriptor::uri)
                .toList();
    }

    private List<String> namesOfPrompts(McpServerManifest manifest) {
        return manifest.prompts().stream()
                .map(McpPromptDescriptor::name)
                .toList();
    }
}
