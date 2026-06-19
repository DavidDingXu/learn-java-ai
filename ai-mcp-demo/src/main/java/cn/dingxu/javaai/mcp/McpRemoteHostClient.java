package cn.dingxu.javaai.mcp;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class McpRemoteHostClient {

    private final String hostName;
    private final McpRemoteEndpoint endpoint;
    private final ToolAccessPolicy accessPolicy;
    private final McpAuditLedger ledger;
    private McpClientSession session;

    public McpRemoteHostClient(String hostName,
                               McpRemoteEndpoint endpoint,
                               ToolAccessPolicy accessPolicy,
                               McpAuditLedger ledger) {
        this.hostName = hostName == null || hostName.isBlank() ? "unknown-host" : hostName;
        this.endpoint = endpoint;
        this.accessPolicy = accessPolicy == null ? ToolAccessPolicy.allowlist(java.util.Set.of()) : accessPolicy;
        this.ledger = ledger == null ? new McpAuditLedger() : ledger;
    }

    public McpClientSession initialize() {
        if (endpoint == null || !endpoint.available()) {
            throw new IllegalStateException("mcp server unavailable");
        }
        McpHostClient client = new McpHostClient(endpoint.server(), accessPolicy, ledger);
        this.session = client.initialize(hostName);
        return session;
    }

    public McpToolResult callTool(String toolName, Map<String, Object> arguments, OperatorContext operator) {
        if (endpoint == null || !endpoint.available()) {
            return McpToolResult.failed("mcp server unavailable", Map.of());
        }
        McpHostClient client = new McpHostClient(endpoint.server(), accessPolicy, ledger);
        return client.callTool(toolName, arguments, operator);
    }

    public McpDebugReport debugReport() {
        if (endpoint == null || !endpoint.available()) {
            return new McpDebugReport(
                    endpoint == null ? "unknown" : endpoint.url(),
                    endpoint == null ? McpTransportType.STREAMABLE_HTTP : endpoint.transportType(),
                    endpoint == null ? java.time.Duration.ofSeconds(5) : endpoint.connectTimeout(),
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    "server unavailable",
                    Instant.now()
            );
        }

        McpClientSession snapshot = session == null ? initialize() : session;
        return new McpDebugReport(
                endpoint.url(),
                endpoint.transportType(),
                endpoint.connectTimeout(),
                true,
                snapshot.tools(),
                snapshot.resources(),
                snapshot.prompts(),
                "",
                Instant.now()
        );
    }
}
