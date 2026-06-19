package cn.dingxu.javaai.tool;

import cn.dingxu.javaai.tool.service.OperatorContext;
import cn.dingxu.javaai.tool.service.TicketToolFacade;
import cn.dingxu.javaai.tool.service.ToolExecutionLedger;
import cn.dingxu.javaai.tool.service.ToolExecutionRecord;
import cn.dingxu.javaai.tool.service.ToolResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketToolFacadeTest {

    @Test
    void lookupTicketReturnsAuditedToolResult() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);

        ToolResult result = facade.lookupTicket("T-1001", new OperatorContext("u1001", "tenant-a", "support"));

        assertThat(result.success()).isTrue();
        assertThat(result.data()).containsEntry("ticketId", "T-1001");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().toolName()).isEqualTo("ticket.lookup");
        assertThat(ledger.records().getFirst().traceId()).isNotBlank();
        assertThat(ledger.records().getFirst().durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void closeTicketRequiresHumanApproval() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);

        ToolResult result = facade.closeTicket("T-1001", false, new OperatorContext("u1001", "tenant-a", "support"));

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("需要人工确认");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().approved()).isFalse();
    }

    @Test
    void closeTicketRequiresConfirmationTokenForApprovedWriteAction() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);

        ToolResult result = facade.closeTicket(
                "T-1001",
                true,
                "",
                new OperatorContext("u1001", "tenant-a", "support")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("confirmationToken");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().approved()).isFalse();
    }

    @Test
    void closeTicketUsesConfirmationTokenAsIdempotencyKey() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);
        OperatorContext operator = new OperatorContext("u1001", "tenant-a", "support");

        ToolResult first = facade.closeTicket("T-1001", true, "confirm-001", operator);
        ToolResult second = facade.closeTicket("T-1001", true, "confirm-001", operator);

        assertThat(first.success()).isTrue();
        assertThat(second.success()).isTrue();
        assertThat(second.message()).contains("重复确认");
        assertThat(ledger.records()).hasSize(2);
        assertThat(ledger.records().getFirst().arguments()).containsEntry("confirmationToken", "confirm-001");
        assertThat(ledger.records().get(1).message()).contains("重复确认");
    }

    @Test
    void lookupOrderReturnsMinimalDataAndAuditArguments() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);

        ToolResult result = facade.lookupOrder("O-1001", new OperatorContext("u1001", "tenant-a", "support"));

        assertThat(result.success()).isTrue();
        assertThat(result.data())
                .containsEntry("orderId", "O-1001")
                .containsEntry("shippingStatus", "SHIPPED");
        assertThat(result.data()).doesNotContainKeys("customerPhone", "paymentAccount");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().toolName()).isEqualTo("order.lookup");
        assertThat(ledger.records().getFirst().arguments()).containsEntry("orderId", "O-1001");
    }

    @Test
    void lookupToolsRejectBlankIdentifiersBeforeAudit() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);
        OperatorContext operator = new OperatorContext("u1001", "tenant-a", "support");

        assertThatThrownBy(() -> facade.lookupTicket(" ", operator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticketId");
        assertThatThrownBy(() -> facade.lookupOrder("", operator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");
        assertThat(ledger.records()).isEmpty();
    }

    @Test
    void lookupToolsRejectMalformedIdentifiersBeforeAudit() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);
        OperatorContext operator = new OperatorContext("u1001", "tenant-a", "support");

        assertThatThrownBy(() -> facade.lookupTicket("ORDER-1001", operator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ticketId must start with T-");
        assertThatThrownBy(() -> facade.lookupOrder("T-1001", operator))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId must start with O-");
        assertThat(ledger.records()).isEmpty();
    }

    @Test
    void closeTicketRejectsMalformedConfirmationToken() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade facade = new TicketToolFacade(ledger);

        ToolResult result = facade.closeTicket(
                "T-1001",
                true,
                "token-001",
                new OperatorContext("u1001", "tenant-a", "support")
        );

        assertThat(result.success()).isFalse();
        assertThat(result.message()).contains("confirmationToken must start with confirm-");
        assertThat(ledger.records()).hasSize(1);
        assertThat(ledger.records().getFirst().success()).isFalse();
    }

    @Test
    void ledgerCanQueryRecordsByTraceTenantAndToolName() {
        ToolExecutionLedger ledger = new ToolExecutionLedger();
        ToolResult ok = ToolResult.ok("ok", java.util.Map.of("ticketId", "T-1001"));
        OperatorContext support = new OperatorContext("u1001", "tenant-a", "support");
        OperatorContext ops = new OperatorContext("u2001", "tenant-b", "ops");

        ToolExecutionRecord first = ledger.append("trace-001", "ticket.lookup", support, true, ok, java.util.Map.of("ticketId", "T-1001"), 12);
        ledger.append("trace-001", "order.lookup", support, true, ok, java.util.Map.of("orderId", "O-1001"), 8);
        ledger.append("trace-002", "ticket.lookup", ops, true, ok, java.util.Map.of("ticketId", "T-2001"), 5);

        assertThat(first.traceId()).isEqualTo("trace-001");
        assertThat(ledger.recordsByTraceId("trace-001")).extracting("toolName")
                .containsExactly("ticket.lookup", "order.lookup");
        assertThat(ledger.recordsByTenant("tenant-a")).hasSize(2);
        assertThat(ledger.recordsByToolName("ticket.lookup")).hasSize(2);
    }
}
