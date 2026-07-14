package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpEnterpriseRegistryTest {

    @Test
    void importsAllowlistedRemoteToolsAsExternalAgentScopeTools() {
        Toolkit toolkit = new Toolkit();
        EnterpriseMcpRegistry registry = new EnterpriseMcpRegistry(
                toolkit, Set.of("crm-mcp"), Set.of("query_customer"));
        McpServerDescriptor server = new McpServerDescriptor("crm-mcp", URI.create("https://mcp.example.com"));
        McpToolDescriptor tool = new McpToolDescriptor(
                "query_customer", "查询客户摘要", true,
                Map.of("type", "object", "properties", Map.of("customer_id", Map.of("type", "string")), "required", List.of("customer_id")));

        McpRegistrationReceipt receipt = registry.register(server, List.of(tool));

        assertEquals(List.of("query_customer"), receipt.registeredTools());
        assertTrue(toolkit.isExternalTool("query_customer"));
    }

    @Test
    void rejectsUntrustedTransportAndUndeclaredTools() {
        EnterpriseMcpRegistry registry = new EnterpriseMcpRegistry(
                new Toolkit(), Set.of("crm-mcp"), Set.of("query_customer"));

        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new McpServerDescriptor("crm-mcp", URI.create("http://mcp.example.com")), List.of()));
        assertThrows(IllegalArgumentException.class, () -> registry.register(
                new McpServerDescriptor("crm-mcp", URI.create("https://mcp.example.com")),
                List.of(new McpToolDescriptor("delete_customer", "删除客户", false, Map.of("type", "object")))));
    }
}
