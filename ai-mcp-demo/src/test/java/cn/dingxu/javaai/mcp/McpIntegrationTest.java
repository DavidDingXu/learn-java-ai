package cn.dingxu.javaai.mcp;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class McpIntegrationTest {

    @Test
    void serverPublishesToolsResourcesAndPrompts() {
        PolicyMcpServer server = PolicyMcpServer.seeded();

        McpServerManifest manifest = server.manifest();

        assertThat(manifest.serverName()).isEqualTo("policy-center-mcp");
        assertThat(manifest.tools()).extracting(McpToolDescriptor::name)
                .containsExactly("policy.search", "policy.get");
        assertThat(manifest.resources()).extracting(McpResourceDescriptor::uri)
                .contains("policy://refund/refund-policy-001");
        assertThat(manifest.prompts()).extracting(McpPromptDescriptor::name)
                .containsExactly("ticket-policy-answer");
    }

    @Test
    void hostClientCallsOnlyAllowlistedToolsAndRecordsAudit() {
        PolicyMcpServer server = PolicyMcpServer.seeded();
        McpAuditLedger ledger = new McpAuditLedger();
        McpHostClient client = new McpHostClient(server, Set.of("policy.search"), ledger);
        OperatorContext operator = new OperatorContext("u1001", "tenant-a", "support");

        McpToolResult result = client.callTool("policy.search", Map.of("query", "退款"), operator);
        McpToolResult denied = client.callTool("policy.get", Map.of("documentId", "refund-policy-001"), operator);

        assertThat(result.success()).isTrue();
        assertThat((List<?>) result.data().get("matches")).hasSize(2);
        assertThat(denied.success()).isFalse();
        assertThat(denied.message()).contains("not allowlisted");
        assertThat(ledger.records()).extracting(McpAuditRecord::toolName)
                .containsExactly("policy.search", "policy.get");
        assertThat(ledger.records()).extracting(McpAuditRecord::allowed)
                .containsExactly(true, false);
    }

    @Test
    void resourceReadIsFilteredByTenantAndDepartment() {
        PolicyMcpServer server = PolicyMcpServer.seeded();
        OperatorContext support = new OperatorContext("u1001", "tenant-a", "support");
        OperatorContext hr = new OperatorContext("u2001", "tenant-a", "hr");

        McpResourceContent visible = server.readResource("policy://refund/refund-policy-001", support);
        McpResourceContent hidden = server.readResource("policy://refund/refund-policy-001", hr);

        assertThat(visible.visible()).isTrue();
        assertThat(visible.content()).contains("发货后退款");
        assertThat(hidden.visible()).isFalse();
        assertThat(hidden.content()).isEmpty();
    }

    @Test
    void promptRenderingKeepsBusinessVariablesExplicit() {
        PolicyMcpServer server = PolicyMcpServer.seeded();

        McpPromptMessage prompt = server.renderPrompt("ticket-policy-answer", Map.of(
                "ticketSummary", "客户申请退款，但订单已经发货。",
                "policyEvidence", List.of("发货后退款需先核对物流状态。")
        ));

        assertThat(prompt.system()).contains("只能基于给定制度依据");
        assertThat(prompt.user()).contains("客户申请退款");
        assertThat(prompt.user()).contains("发货后退款需先核对物流状态");
    }

    @Test
    void hostClientInitializationKeepsDiscoveredCapabilitiesAsSnapshot() {
        PolicyMcpServer server = PolicyMcpServer.seeded();
        McpHostClient client = new McpHostClient(server, Set.of("policy.search"), new McpAuditLedger());

        McpClientSession session = client.initialize("helpdesk-agent-host");

        assertThat(session.hostName()).isEqualTo("helpdesk-agent-host");
        assertThat(session.serverName()).isEqualTo("policy-center-mcp");
        assertThat(session.tools()).containsExactly("policy.search", "policy.get");
        assertThat(session.resources()).contains("policy://refund/refund-policy-001");
        assertThat(session.prompts()).containsExactly("ticket-policy-answer");
    }

    @Test
    void toolPolicyDeniesSensitiveToolWhenOperatorDoesNotHaveRequiredPermission() {
        PolicyMcpServer server = PolicyMcpServer.seeded();
        McpAuditLedger ledger = new McpAuditLedger();
        ToolAccessPolicy accessPolicy = ToolAccessPolicy.builder()
                .allowTool("policy.search")
                .allowTool("policy.get")
                .requirePermission("policy.get", "policy:read-detail")
                .build();
        McpHostClient client = new McpHostClient(server, accessPolicy, ledger);
        OperatorContext operator = new OperatorContext("u1001", "tenant-a", "support");

        McpToolResult result = client.callTool("policy.get", Map.of("documentId", "refund-policy-001"), operator);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("missing permission");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().get(0).allowed()).isFalse();
        assertThat(ledger.records().get(0).denyReason()).contains("policy:read-detail");
    }

    @Test
    void toolPolicyAllowsSensitiveToolWhenOperatorHasRequiredPermission() {
        PolicyMcpServer server = PolicyMcpServer.seeded();
        McpAuditLedger ledger = new McpAuditLedger();
        ToolAccessPolicy accessPolicy = ToolAccessPolicy.builder()
                .allowTool("policy.search")
                .allowTool("policy.get")
                .requirePermission("policy.get", "policy:read-detail")
                .build();
        McpHostClient client = new McpHostClient(server, accessPolicy, ledger);
        OperatorContext operator = new OperatorContext(
                "u1001",
                "tenant-a",
                "support",
                Set.of("policy:read-detail")
        );

        McpToolResult result = client.callTool("policy.get", Map.of("documentId", "refund-policy-001"), operator);

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsKey("content");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().get(0).allowed()).isTrue();
        assertThat(ledger.records().get(0).denyReason()).isBlank();
    }
}
