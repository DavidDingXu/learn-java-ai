package com.xiaoding.javaai.agent.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class TicketAgent {

    public TicketAgentResult handle(TicketAgentRequest request) {
        List<AgentStep> steps = new ArrayList<>();

        Map<String, Object> ticket = lookupTicket(request.ticketId());
        steps.add(new AgentStep(AgentStepType.LOOKUP_TICKET, "查询当前工单", ticket));

        Map<String, Object> order = lookupOrder(ticket);
        steps.add(new AgentStep(AgentStepType.LOOKUP_ORDER, "查询订单状态", order));

        Map<String, Object> policy = retrievePolicy(request.userQuestion(), request.tenantId(), request.department());
        steps.add(new AgentStep(AgentStepType.RETRIEVE_POLICY, "检索退款制度", policy));

        Map<String, Object> risk = assessRisk(request, ticket, order);
        steps.add(new AgentStep(AgentStepType.ASSESS_RISK, "评估自动执行风险", risk));

        String advice = composeAdvice(ticket, order, policy, risk);
        Map<String, Object> adviceOutput = Map.of(
                "advice", advice,
                "requiresHumanApproval", true
        );
        steps.add(new AgentStep(AgentStepType.COMPOSE_ADVICE, "生成处理建议", adviceOutput));

        return new TicketAgentResult(advice, true, steps);
    }

    private Map<String, Object> lookupTicket(String ticketId) {
        return Map.of(
                "ticketId", ticketId,
                "status", "OPEN",
                "orderStatus", "SHIPPED",
                "question", "客户申请退款，但订单已经发货。"
        );
    }

    private Map<String, Object> lookupOrder(Map<String, Object> ticket) {
        return Map.of(
                "orderId", "O-" + ticket.get("ticketId"),
                "status", ticket.get("orderStatus"),
                "amount", "5000"
        );
    }

    private Map<String, Object> retrievePolicy(String question, String tenantId, String department) {
        return Map.of(
                "tenantId", tenantId,
                "department", department,
                "matchedDocument", "refund-policy-001",
                "citation", "发货后退款需先核对物流状态，高金额退款必须转人工复核。"
        );
    }

    private Map<String, Object> assessRisk(TicketAgentRequest request, Map<String, Object> ticket, Map<String, Object> order) {
        boolean highRisk = request.userQuestion().contains("5000")
                || request.userQuestion().contains("关闭")
                || "SHIPPED".equals(order.get("status"));
        return Map.of(
                "riskLevel", highRisk ? "HIGH" : "MEDIUM",
                "autoExecutable", false,
                "reason", highRisk ? "high_value_refund_or_close_ticket" : "refund_requires_manual_review",
                "ticketStatus", ticket.get("status"),
                "orderStatus", order.get("status")
        );
    }

    private String composeAdvice(Map<String, Object> ticket, Map<String, Object> order, Map<String, Object> policy, Map<String, Object> risk) {
        return """
                建议先核对物流状态，确认客户是否拒收、退回或已签收。
                当前订单状态为 %s，风险等级为 %s，不能让 Agent 直接执行退款或关闭工单。
                如涉及高金额退款、关闭工单或状态不一致，需要人工确认后再执行后续动作。
                依据：%s
                """.formatted(order.get("status"), risk.get("riskLevel"), policy.get("citation")).trim();
    }
}
