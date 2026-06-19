package com.xiaoding.javaai.helpdesk.agent;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HelpdeskAgentApplicationService {

    private final TicketToolFacade tools;
    private final ToolExecutionLedger toolLedger;

    public HelpdeskAgentApplicationService(TicketToolFacade tools, ToolExecutionLedger toolLedger) {
        this.tools = tools;
        this.toolLedger = toolLedger;
    }

    public static HelpdeskAgentApplicationService seeded() {
        TicketRepository ticketRepository = new TicketRepository();
        ticketRepository.save(new Ticket(
                "T-1001",
                "O-9001",
                "tenant-a",
                "support",
                TicketStatus.OPEN,
                "客户申请退款，但订单已经发货。",
                1800
        ));
        ticketRepository.save(new Ticket(
                "T-2001",
                "O-9002",
                "tenant-a",
                "support",
                TicketStatus.OPEN,
                "员工询问薪资调整规则。",
                0
        ));

        OrderRepository orderRepository = new OrderRepository();
        orderRepository.save(new Order("O-9001", "tenant-a", OrderShippingStatus.SHIPPED, 1800));
        orderRepository.save(new Order("O-9002", "tenant-a", OrderShippingStatus.CREATED, 0));

        PolicyKnowledgeBase knowledgeBase = new PolicyKnowledgeBase();
        knowledgeBase.add(new PolicyDocument(
                "refund-policy-2026",
                "tenant-a",
                Set.of("support", "ops"),
                "退款制度：发货后退款需先核对物流状态。高金额退款必须转人工复核。"
        ));
        knowledgeBase.add(new PolicyDocument(
                "salary-policy-2026",
                "tenant-a",
                Set.of("hr"),
                "薪资制度：薪资调整仅 HR 和授权管理者可见。"
        ));

        ToolExecutionLedger ledger = new ToolExecutionLedger();
        TicketToolFacade tools = new TicketToolFacade(ticketRepository, orderRepository, knowledgeBase, ledger);
        return new HelpdeskAgentApplicationService(tools, ledger);
    }

    public AgentAdviceResult advise(AgentAdviceRequest request) {
        int ledgerOffset = toolLedger.records().size();
        List<TraceStep> traceSteps = new ArrayList<>();

        Ticket ticket = tools.lookupTicket(request.ticketId(), request.operator());
        traceSteps.add(new TraceStep("ticket.lookup", Map.of("ticketId", ticket.ticketId(), "status", ticket.status().name())));

        Order order = tools.lookupOrder(ticket.orderId(), request.operator());
        traceSteps.add(new TraceStep("order.lookup", Map.of("orderId", order.orderId(), "shippingStatus", order.shippingStatus().name())));

        List<Citation> citations = tools.searchPolicy(request.question(), request.operator());
        traceSteps.add(new TraceStep("policy.search", Map.of("citationCount", citations.size())));

        RiskLevel riskLevel = calculateRisk(ticket, order, citations);
        RequiredAction requiredAction = decideAction(riskLevel, citations);
        String advice = composeAdvice(ticket, order, citations, requiredAction);
        traceSteps.add(new TraceStep("advice.compose", Map.of("riskLevel", riskLevel.name())));
        traceSteps.add(new TraceStep("approval.plan", Map.of("requiredAction", requiredAction.name())));

        return new AgentAdviceResult(
                advice,
                riskLevel,
                requiredAction,
                citations,
                toolLedger.records().subList(ledgerOffset, toolLedger.records().size()),
                new AiTrace(null, traceSteps)
        );
    }

    public HelpdeskAgentScenarioReport runRefundScenario() {
        AgentAdviceRequest request = new AgentAdviceRequest(
                "T-1001",
                "客户申请退款，但订单已经发货，应该怎么处理？",
                new OperatorContext("u1001", "tenant-a", "support")
        );
        AgentAdviceResult result = advise(request);
        return new HelpdeskAgentScenarioReport(
                request.ticketId(),
                result.advice(),
                result.riskLevel(),
                result.requiredAction(),
                result.citations().stream().map(Citation::documentId).toList(),
                result.toolRecords().stream().map(ToolExecutionRecord::toolName).toList(),
                result.trace().steps().stream().map(TraceStep::name).toList()
        );
    }

    public ToolResult closeTicket(String ticketId, boolean humanApproved, OperatorContext operator) {
        return tools.closeTicket(ticketId, humanApproved, operator);
    }

    public ToolResult closeTicket(String ticketId, boolean humanApproved, String confirmationToken, OperatorContext operator) {
        return tools.closeTicket(ticketId, humanApproved, confirmationToken, operator);
    }

    public ToolExecutionLedger toolLedger() {
        return toolLedger;
    }

    public AgentEvalReport evaluate(List<AgentEvalCase> cases) {
        if (cases == null || cases.isEmpty()) {
            return new AgentEvalReport(0, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        int citationHits = 0;
        int actionHits = 0;
        for (AgentEvalCase evalCase : cases) {
            AgentAdviceResult result = advise(evalCase.request());
            if (result.citations().stream().anyMatch(citation -> citation.documentId().equals(evalCase.expectedCitationDocumentId()))) {
                citationHits++;
            }
            if (result.requiredAction() == evalCase.expectedAction()) {
                actionHits++;
            }
        }

        return new AgentEvalReport(
                cases.size(),
                rate(citationHits, cases.size()),
                rate(actionHits, cases.size())
        );
    }

    private RiskLevel calculateRisk(Ticket ticket, Order order, List<Citation> citations) {
        if (citations.isEmpty()) {
            return RiskLevel.MEDIUM;
        }
        if (order.shippingStatus() == OrderShippingStatus.SHIPPED || ticket.refundAmount() >= 1000) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.LOW;
    }

    private RequiredAction decideAction(RiskLevel riskLevel, List<Citation> citations) {
        if (citations.isEmpty()) {
            return RequiredAction.MANUAL_REVIEW;
        }
        if (riskLevel == RiskLevel.HIGH) {
            return RequiredAction.MANUAL_REVIEW;
        }
        return RequiredAction.ASK_CUSTOMER_CONFIRM;
    }

    private String composeAdvice(Ticket ticket, Order order, List<Citation> citations, RequiredAction action) {
        if (citations.isEmpty()) {
            return "没有检索到当前用户可访问的制度依据，不能直接生成确定性处理结论。建议转人工复核。";
        }
        return """
                建议先核对物流状态，确认客户是否拒收、退回或已签收。
                当前订单状态为 %s，工单金额为 %d，Agent 不能直接执行退款或关闭工单。
                下一步动作：%s。
                依据：%s
                """.formatted(
                order.shippingStatus(),
                ticket.refundAmount(),
                action,
                citations.getFirst().quote()
        ).trim();
    }

    private BigDecimal rate(int hits, int total) {
        return BigDecimal.valueOf(hits).divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
