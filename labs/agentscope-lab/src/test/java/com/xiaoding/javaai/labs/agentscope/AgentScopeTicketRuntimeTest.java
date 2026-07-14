package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.permission.PermissionBehavior;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentScopeTicketRuntimeTest {

    @Test
    void reusesToolIdentityAndConfirmationBoundaries() {
        AgentScopeTicketRuntime runtime = AgentScopeTicketRuntime.createDefault(new TicketBusinessTools());
        AgentExecutionIdentity identity = new AgentExecutionIdentity("tenant-a", "agent-user", "customer_service");

        ToolAuthorizationDecision read = runtime.authorize(identity, "query_ticket", Map.of("ticket_id", "T-1"));
        ToolAuthorizationDecision write = runtime.authorize(identity, "update_ticket", Map.of("ticket_id", "T-1", "status", "CLOSED"));
        ToolAuthorizationDecision export = runtime.authorize(identity, "export_all_customers", Map.of());

        assertEquals(PermissionBehavior.ALLOW, read.behavior());
        assertEquals(PermissionBehavior.ASK, write.behavior());
        assertEquals(PermissionBehavior.DENY, export.behavior());
        assertEquals("tenant-a", read.tenantId());
    }
}
