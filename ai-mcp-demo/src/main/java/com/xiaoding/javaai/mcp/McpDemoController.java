package com.xiaoding.javaai.mcp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/mcp")
public class McpDemoController {

    private final PolicyMcpServer server = PolicyMcpServer.seeded();
    private final McpAuditLedger ledger = new McpAuditLedger();
    private final ToolAccessPolicy accessPolicy = ToolAccessPolicy.builder()
            .allowTool("policy.search")
            .allowTool("policy.get")
            .requirePermission("policy.get", "policy:read-detail")
            .build();
    private final McpRemoteEndpoint endpoint = new McpRemoteEndpoint(
            "https://example.local/mcp/policy-center",
            McpTransportType.STREAMABLE_HTTP,
            server,
            Duration.ofSeconds(3)
    );

    @GetMapping("/session")
    public McpClientSession session() {
        return hostClient().initialize("helpdesk-agent-host");
    }

    @PostMapping("/tools/call")
    public McpToolResult callTool(@RequestBody ToolCallRequest request) {
        return hostClient().callTool(request.toolName(), request.arguments(), request.operator().toOperatorContext());
    }

    @PostMapping("/resources/read")
    public McpResourceContent readResource(@RequestBody ResourceReadRequest request) {
        return server.readResource(request.uri(), request.operator().toOperatorContext());
    }

    @GetMapping("/debug")
    public McpDebugReport debug() {
        return remoteClient().debugReport();
    }

    @GetMapping("/audit")
    public java.util.List<McpAuditRecord> audit() {
        return ledger.records();
    }

    private McpHostClient hostClient() {
        return new McpHostClient(server, accessPolicy, ledger);
    }

    private McpRemoteHostClient remoteClient() {
        return new McpRemoteHostClient("helpdesk-agent-host", endpoint, accessPolicy, ledger);
    }

    public record ToolCallRequest(
            String toolName,
            Map<String, Object> arguments,
            OperatorView operator
    ) {
        public ToolCallRequest {
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
            operator = operator == null ? OperatorView.defaultSupport() : operator;
        }
    }

    public record ResourceReadRequest(
            String uri,
            OperatorView operator
    ) {
        public ResourceReadRequest {
            operator = operator == null ? OperatorView.defaultSupport() : operator;
        }
    }

    public record OperatorView(
            String userId,
            String tenantId,
            String department,
            Set<String> permissions
    ) {
        public static OperatorView defaultSupport() {
            return new OperatorView("u1001", "tenant-a", "support", Set.of());
        }

        public OperatorContext toOperatorContext() {
            return new OperatorContext(
                    valueOrDefault(userId, "u1001"),
                    valueOrDefault(tenantId, "tenant-a"),
                    valueOrDefault(department, "support"),
                    permissions == null ? Set.of() : permissions
            );
        }

        private String valueOrDefault(String value, String defaultValue) {
            return value == null || value.isBlank() ? defaultValue : value;
        }
    }
}
