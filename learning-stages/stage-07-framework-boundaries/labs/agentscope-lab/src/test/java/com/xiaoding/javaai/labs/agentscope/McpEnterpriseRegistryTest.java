package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpEnterpriseRegistryTest {

    @Test
    void separatesToolRegistrationFromInvocationPermission() {
        Toolkit toolkit = new Toolkit();
        EnterpriseMcpRegistry registry = new EnterpriseMcpRegistry(
                toolkit,
                Map.of("crm-mcp", URI.create("https://mcp.example.com/v1")),
                Set.of("query_customer", "query_customer_notes"));
        McpServerDescriptor server = new McpServerDescriptor("crm-mcp");
        McpToolDescriptor customer = new McpToolDescriptor(
                "query_customer", "查询客户摘要", true,
                Map.of("type", "object", "properties", Map.of("customer_id", Map.of("type", "string")), "required", List.of("customer_id")));
        McpToolDescriptor notes = new McpToolDescriptor(
                "query_customer_notes", "查询客户备注", true,
                Map.of("type", "object", "properties", Map.of("customer_id", Map.of("type", "string")), "required", List.of("customer_id")));

        McpRegistrationReceipt receipt = registry.register(server, List.of(customer, notes));
        PermissionContextState permissions = PermissionContextState.builder()
                .mode(PermissionMode.DONT_ASK)
                .addAllowRule("query_customer",
                        new PermissionRule("query_customer", null, PermissionBehavior.ALLOW, "crm-read-policy"))
                .build();
        AgentScopeTicketRuntime runtime = AgentScopeTicketRuntime.create(toolkit, permissions);
        AgentExecutionIdentity identity = new AgentExecutionIdentity(
                "tenant-a", "agent-user", "customer_service");

        ToolAuthorizationDecision allowed = runtime.authorize(
                identity, "query_customer", Map.of("customer_id", "C-1"));
        ToolAuthorizationDecision denied = runtime.authorize(
                identity, "query_customer_notes", Map.of("customer_id", "C-1"));

        assertEquals(List.of("query_customer", "query_customer_notes"), receipt.registeredTools());
        assertEquals(URI.create("https://mcp.example.com/v1"), receipt.endpoint());
        assertTrue(toolkit.isExternalTool("query_customer"));
        assertEquals(PermissionBehavior.ALLOW, allowed.behavior());
        assertEquals("tenant-a", allowed.tenantId());
        assertEquals("query_customer", allowed.toolName());
        assertEquals("crm-read-policy", allowed.ruleSource());
        assertEquals(PermissionBehavior.DENY, denied.behavior());
        assertEquals("permission-engine-default", denied.ruleSource());
    }

    @Test
    void rejectsUntrustedTransportAndUndeclaredTools() {
        assertThrows(IllegalArgumentException.class, () -> new EnterpriseMcpRegistry(
                new Toolkit(), Map.of("crm-mcp", URI.create("http://mcp.example.com")),
                Set.of("query_customer")));

        EnterpriseMcpRegistry registry = new EnterpriseMcpRegistry(new Toolkit(),
                Map.of("crm-mcp", URI.create("https://mcp.example.com/v1")),
                Set.of("query_customer"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new McpServerDescriptor("unknown-mcp"), List.of()));
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new McpServerDescriptor("crm-mcp"),
                List.of(new McpToolDescriptor("delete_customer", "删除客户", false, Map.of("type", "object")))));
    }

    @Test
    void rejectsTheWholeDiscoveryBatchBeforeRegisteringAnyTool() {
        Toolkit toolkit = new Toolkit();
        EnterpriseMcpRegistry registry = new EnterpriseMcpRegistry(
                toolkit,
                Map.of("crm-mcp", URI.create("https://mcp.example.com/v1")),
                Set.of("query_customer"));
        McpToolDescriptor approved = new McpToolDescriptor(
                "query_customer", "查询客户摘要", true, Map.of("type", "object"));
        McpToolDescriptor unexpected = new McpToolDescriptor(
                "export_customers", "导出客户", true, Map.of("type", "object"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new McpServerDescriptor("crm-mcp"), List.of(approved, unexpected)));

        assertFalse(toolkit.isExternalTool("query_customer"));
    }
}
