package cn.dingxu.javaai.tool.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TicketToolFacade {

    private final ToolExecutionLedger ledger;
    private final ToolParameterValidator parameterValidator;
    private final Map<String, ToolResult> completedConfirmations = new ConcurrentHashMap<>();

    public TicketToolFacade(ToolExecutionLedger ledger) {
        this(ledger, new ToolParameterValidator());
    }

    public TicketToolFacade(ToolExecutionLedger ledger, ToolParameterValidator parameterValidator) {
        this.ledger = ledger;
        this.parameterValidator = parameterValidator;
    }

    public ToolResult lookupTicket(String ticketId, OperatorContext operator) {
        parameterValidator.requireTicketId(ticketId);
        ToolResult result = ToolResult.ok("工单查询成功", Map.of(
                "ticketId", ticketId,
                "tenantId", operator.tenantId(),
                "status", "OPEN",
                "customerQuestion", "客户申请退款，但订单已经发货。",
                "risk", "MEDIUM"
        ));
        ledger.append("ticket.lookup", operator, true, result, Map.of("ticketId", ticketId));
        return result;
    }

    public ToolResult closeTicket(String ticketId, boolean humanApproved, OperatorContext operator) {
        return closeTicket(ticketId, humanApproved, "", operator);
    }

    public ToolResult closeTicket(String ticketId, boolean humanApproved, String confirmationToken, OperatorContext operator) {
        parameterValidator.requireTicketId(ticketId);
        Map<String, Object> arguments = closeArguments(ticketId, confirmationToken);
        if (!humanApproved) {
            ToolResult result = ToolResult.failed("关闭工单属于写操作，需要人工确认后才能执行。", Map.of(
                    "ticketId", ticketId,
                    "requiredApproval", true
            ));
            ledger.append("ticket.close", operator, false, result, arguments);
            return result;
        }
        if (confirmationToken == null || confirmationToken.isBlank()) {
            ToolResult result = ToolResult.failed("关闭工单需要有效 confirmationToken，不能只依赖 humanApproved=true。", Map.of(
                    "ticketId", ticketId,
                    "requiredConfirmationToken", true
            ));
            ledger.append("ticket.close", operator, false, result, arguments);
            return result;
        }
        try {
            parameterValidator.requireConfirmationToken(confirmationToken);
        } catch (IllegalArgumentException ex) {
            ToolResult result = ToolResult.failed(ex.getMessage(), Map.of(
                    "ticketId", ticketId,
                    "requiredConfirmationToken", true
            ));
            ledger.append("ticket.close", operator, false, result, arguments);
            return result;
        }
        ToolResult existing = completedConfirmations.get(confirmationToken);
        if (existing != null) {
            ToolResult result = ToolResult.ok("重复确认请求已忽略，工单关闭结果不重复执行。", existing.data());
            ledger.append("ticket.close", operator, true, result, arguments);
            return result;
        }

        ToolResult result = ToolResult.ok("工单已关闭", Map.of(
                "ticketId", ticketId,
                "status", "CLOSED"
        ));
        completedConfirmations.put(confirmationToken, result);
        ledger.append("ticket.close", operator, true, result, arguments);
        return result;
    }

    public ToolResult lookupOrder(String orderId, OperatorContext operator) {
        parameterValidator.requireOrderId(orderId);
        ToolResult result = ToolResult.ok("订单查询成功", Map.of(
                "orderId", orderId,
                "shippingStatus", "SHIPPED",
                "refundAllowedActions", Set.of("ASK_CUSTOMER_CONFIRM_RECEIVE", "TRANSFER_MANUAL_REVIEW")
        ));
        ledger.append("order.lookup", operator, true, result, Map.of("orderId", orderId));
        return result;
    }

    private Map<String, Object> closeArguments(String ticketId, String confirmationToken) {
        if (confirmationToken == null || confirmationToken.isBlank()) {
            return Map.of("ticketId", ticketId);
        }
        return Map.of("ticketId", ticketId, "confirmationToken", confirmationToken);
    }
}
