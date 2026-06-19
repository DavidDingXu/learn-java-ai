package cn.dingxu.javaai.helpdesk.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class TicketToolFacade {

    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final PolicyKnowledgeBase knowledgeBase;
    private final ToolExecutionLedger ledger;
    private final Map<String, ToolResult> completedConfirmations = new ConcurrentHashMap<>();

    TicketToolFacade(TicketRepository ticketRepository,
                     OrderRepository orderRepository,
                     PolicyKnowledgeBase knowledgeBase,
                     ToolExecutionLedger ledger) {
        this.ticketRepository = ticketRepository;
        this.orderRepository = orderRepository;
        this.knowledgeBase = knowledgeBase;
        this.ledger = ledger;
    }

    Ticket lookupTicket(String ticketId, OperatorContext operator) {
        Ticket ticket = ticketRepository.getVisible(ticketId, operator);
        ToolResult result = ToolResult.ok("工单查询成功", Map.of(
                "ticketId", ticket.ticketId(),
                "orderId", ticket.orderId(),
                "status", ticket.status().name()
        ));
        ledger.append("ticket.lookup", operator, true, result, Map.of("ticketId", ticketId));
        return ticket;
    }

    Order lookupOrder(String orderId, OperatorContext operator) {
        Order order = orderRepository.get(orderId, operator);
        ToolResult result = ToolResult.ok("订单查询成功", Map.of(
                "orderId", order.orderId(),
                "shippingStatus", order.shippingStatus().name(),
                "amount", order.amount()
        ));
        ledger.append("order.lookup", operator, true, result, Map.of("orderId", orderId));
        return order;
    }

    List<Citation> searchPolicy(String question, OperatorContext operator) {
        List<Citation> citations = knowledgeBase.search(question, operator);
        ToolResult result = ToolResult.ok("制度检索完成", Map.of(
                "citationCount", citations.size()
        ));
        ledger.append("policy.search", operator, true, result, Map.of("question", question));
        return citations;
    }

    ToolResult closeTicket(String ticketId, boolean humanApproved, OperatorContext operator) {
        return closeTicket(ticketId, humanApproved, "", operator);
    }

    ToolResult closeTicket(String ticketId, boolean humanApproved, String confirmationToken, OperatorContext operator) {
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
        if (!confirmationToken.startsWith("confirm-")) {
            ToolResult result = ToolResult.failed("confirmationToken must start with confirm-", Map.of(
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

        Ticket ticket = ticketRepository.getVisible(ticketId, operator);
        if (ticket.status() == TicketStatus.CLOSED) {
            ToolResult result = ToolResult.failed("工单已关闭，不能重复关闭。", Map.of(
                    "ticketId", ticketId,
                    "status", TicketStatus.CLOSED.name()
            ));
            ledger.append("ticket.close", operator, false, result, arguments);
            return result;
        }
        ticketRepository.close(ticketId, operator);
        ToolResult result = ToolResult.ok("工单已关闭", Map.of(
                "ticketId", ticketId,
                "status", TicketStatus.CLOSED.name()
        ));
        completedConfirmations.put(confirmationToken, result);
        ledger.append("ticket.close", operator, true, result, arguments);
        return result;
    }

    private Map<String, Object> closeArguments(String ticketId, String confirmationToken) {
        if (confirmationToken == null || confirmationToken.isBlank()) {
            return Map.of("ticketId", ticketId);
        }
        return Map.of("ticketId", ticketId, "confirmationToken", confirmationToken);
    }
}
