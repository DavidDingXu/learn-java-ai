package cn.dingxu.javaai.mcp;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpRemoteTransportTest {

    @Test
    void remoteEndpointCreatesSessionAndDebugReportFromManifestSnapshot() {
        PolicyMcpServer server = PolicyMcpServer.seeded();
        McpAuditLedger ledger = new McpAuditLedger();
        McpRemoteEndpoint endpoint = new McpRemoteEndpoint(
                "https://mcp.example.local/policy",
                McpTransportType.STREAMABLE_HTTP,
                server,
                Duration.ofSeconds(3)
        );
        McpRemoteHostClient client = new McpRemoteHostClient(
                "helpdesk-agent-host",
                endpoint,
                ToolAccessPolicy.allowlist(Set.of("policy.search")),
                ledger
        );

        McpClientSession session = client.initialize();
        McpDebugReport report = client.debugReport();

        assertThat(session.serverName()).isEqualTo("policy-center-mcp");
        assertThat(report.endpoint()).isEqualTo("https://mcp.example.local/policy");
        assertThat(report.transportType()).isEqualTo(McpTransportType.STREAMABLE_HTTP);
        assertThat(report.connectTimeout()).isEqualTo(Duration.ofSeconds(3));
        assertThat(report.tools()).containsExactly("policy.search", "policy.get");
        assertThat(report.resources()).contains("policy://refund/refund-policy-001");
        assertThat(report.prompts()).containsExactly("ticket-policy-answer");
        assertThat(report.connected()).isTrue();
    }

    @Test
    void debugReportMarksEndpointDisconnectedWhenServerIsMissing() {
        McpRemoteEndpoint endpoint = new McpRemoteEndpoint(
                "https://mcp.example.local/missing",
                McpTransportType.SSE,
                null,
                Duration.ofSeconds(1)
        );
        McpRemoteHostClient client = new McpRemoteHostClient(
                "helpdesk-agent-host",
                endpoint,
                ToolAccessPolicy.allowlist(Set.of("policy.search")),
                new McpAuditLedger()
        );

        McpDebugReport report = client.debugReport();

        assertThat(report.connected()).isFalse();
        assertThat(report.error()).contains("server unavailable");
        assertThat(report.tools()).isEmpty();
    }
}
