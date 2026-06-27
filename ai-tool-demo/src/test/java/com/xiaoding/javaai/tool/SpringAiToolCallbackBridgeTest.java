package com.xiaoding.javaai.tool;

import com.xiaoding.javaai.tool.service.TicketToolFacade;
import com.xiaoding.javaai.tool.service.ToolExecutionLedger;
import com.xiaoding.javaai.tool.service.springai.SpringAiToolCallbackBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiToolCallbackBridgeTest {

    @Test
    void exposesGovernedTicketToolsAsSpringAiToolCallbacks() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        SpringAiToolCallbackBridge bridge = new SpringAiToolCallbackBridge(new TicketToolFacade(ledger));

        ToolCallback[] callbacks = bridge.callbacks();

        assertThat(callbacks).extracting(callback -> callback.getToolDefinition().name())
                .containsExactly("ticket.lookup", "ticket.close");
        assertThat(callbacks[0].getToolDefinition().inputSchema())
                .contains("ticketId")
                .contains("operator")
                .contains("tenantId");

        ToolCallback lookup = find(callbacks, "ticket.lookup");
        String output = lookup.call("""
                {
                  "ticketId": "T-1001",
                  "operator": {
                    "userId": "u1001",
                    "tenantId": "tenant-a",
                    "department": "support"
                  }
                }
                """);

        assertThat(output).contains("工单查询成功").contains("T-1001");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().toolName()).isEqualTo("ticket.lookup");
    }

    @Test
    void springAiToolCallbackDoesNotBypassWriteConfirmation() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        SpringAiToolCallbackBridge bridge = new SpringAiToolCallbackBridge(new TicketToolFacade(ledger));

        ToolCallback close = find(bridge.callbacks(), "ticket.close");
        String output = close.call("""
                {
                  "ticketId": "T-1001",
                  "humanApproved": false,
                  "operator": {
                    "userId": "u1001",
                    "tenantId": "tenant-a",
                    "department": "support"
                  }
                }
                """);

        assertThat(output).contains("需要人工确认");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().toolName()).isEqualTo("ticket.close");
        assertThat(ledger.records().getFirst().approved()).isFalse();
        assertThat(ledger.records().getFirst().success()).isFalse();
    }

    @Test
    void toolCallbackProviderKeepsSameCallbacksForChatClientIntegration() {
        SpringAiToolCallbackBridge bridge = new SpringAiToolCallbackBridge(new TicketToolFacade(new ToolExecutionLedger()));

        assertThat(bridge.toolCallbackProvider().getToolCallbacks())
                .extracting(callback -> callback.getToolDefinition().name())
                .containsExactly("ticket.lookup", "ticket.close");
    }

    private ToolCallback find(ToolCallback[] callbacks, String name) {
        return Arrays.stream(callbacks)
                .filter(callback -> callback.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
