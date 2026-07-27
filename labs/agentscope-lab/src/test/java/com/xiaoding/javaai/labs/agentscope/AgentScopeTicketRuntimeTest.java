package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.permission.PermissionBehavior;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AgentScopeTicketRuntimeTest {

    @Test
    void mapsRegisteredToolsAndTrustedIdentityToPermissionDecisions() {
        AgentScopeTicketRuntime runtime = AgentScopeTicketRuntime.createDefault(new TicketBusinessTools());
        AgentExecutionIdentity identity = new AgentExecutionIdentity("tenant-a", "agent-user", "customer_service");

        ToolAuthorizationDecision read = runtime.authorize(identity, "query_ticket", Map.of("ticket_id", "T-1"));
        ToolAuthorizationDecision write = runtime.authorize(identity, "update_ticket", Map.of("ticket_id", "T-1", "status", "CLOSED"));
        ToolAuthorizationDecision export = runtime.authorize(identity, "export_all_customers", Map.of());

        assertEquals(PermissionBehavior.ALLOW, read.behavior());
        assertEquals(PermissionBehavior.ASK, write.behavior());
        assertEquals(PermissionBehavior.DENY, export.behavior());
        assertEquals("tenant-a", read.tenantId());
        assertEquals("agent-user", read.subjectId());
        assertEquals("query_ticket", read.toolName());
        assertEquals("ticket-policy", read.ruleSource());
        assertEquals("Permission granted for query_ticket", read.reason());
        assertEquals("tenant-a", write.tenantId());
        assertEquals("agent-user", write.subjectId());
        assertEquals("update_ticket", write.toolName());
        assertEquals("ticket-policy", write.ruleSource());
        assertEquals("Permission required for update_ticket", write.reason());
        assertEquals("tenant-a", export.tenantId());
        assertEquals("agent-user", export.subjectId());
        assertEquals("export_all_customers", export.toolName());
        assertEquals("data-policy", export.ruleSource());
        assertEquals("Permission to use export_all_customers has been denied", export.reason());
    }

    @Test
    void rejectsUnknownToolsBeforePermissionEvaluation() {
        AgentScopeTicketRuntime runtime = AgentScopeTicketRuntime.createDefault(new TicketBusinessTools());
        AgentExecutionIdentity identity = new AgentExecutionIdentity(
                "tenant-a", "agent-user", "customer_service");

        assertThrows(IllegalArgumentException.class,
                () -> runtime.authorize(identity, "missing_tool", Map.of()));
    }
}
