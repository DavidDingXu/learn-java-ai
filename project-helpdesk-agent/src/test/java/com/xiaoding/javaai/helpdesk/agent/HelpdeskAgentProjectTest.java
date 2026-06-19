package com.xiaoding.javaai.helpdesk.agent;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelpdeskAgentProjectTest {

    @Test
    void handlesRefundTicketWithPolicyCitationToolAuditAndTrace() {
        HelpdeskAgentApplicationService service = HelpdeskAgentApplicationService.seeded();

        AgentAdviceResult result = service.advise(new AgentAdviceRequest(
                "T-1001",
                "客户申请退款，但订单已经发货，应该怎么处理？",
                new OperatorContext("u1001", "tenant-a", "support")
        ));

        assertThat(result.advice()).contains("核对物流状态");
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.requiredAction()).isEqualTo(RequiredAction.MANUAL_REVIEW);
        assertThat(result.citations()).extracting(Citation::documentId)
                .containsExactly("refund-policy-2026");
        assertThat(result.toolRecords()).extracting(ToolExecutionRecord::toolName)
                .containsExactly("ticket.lookup", "order.lookup", "policy.search");
        assertThat(result.trace().steps()).extracting(TraceStep::name)
                .containsExactly(
                        "ticket.lookup",
                        "order.lookup",
                        "policy.search",
                        "advice.compose",
                        "approval.plan"
                );
    }

    @Test
    void refusesPolicyAccessAcrossDepartment() {
        HelpdeskAgentApplicationService service = HelpdeskAgentApplicationService.seeded();

        AgentAdviceResult result = service.advise(new AgentAdviceRequest(
                "T-2001",
                "员工问薪资调整规则，客服能不能直接回答？",
                new OperatorContext("u1001", "tenant-a", "support")
        ));

        assertThat(result.advice()).contains("没有检索到当前用户可访问的制度依据");
        assertThat(result.citations()).isEmpty();
        assertThat(result.toolRecords()).extracting(ToolExecutionRecord::toolName)
                .contains("policy.search");
    }

    @Test
    void closeTicketRequiresHumanApprovalAndIsAudited() {
        HelpdeskAgentApplicationService service = HelpdeskAgentApplicationService.seeded();

        ToolResult denied = service.closeTicket("T-1001", false, new OperatorContext("u1001", "tenant-a", "support"));
        ToolResult approved = service.closeTicket(
                "T-1001",
                true,
                "confirm-close-1001",
                new OperatorContext("lead-1", "tenant-a", "support")
        );

        assertThat(denied.success()).isFalse();
        assertThat(denied.message()).contains("需要人工确认");
        assertThat(approved.success()).isTrue();
        assertThat(approved.data()).containsEntry("status", "CLOSED");
        assertThat(service.toolLedger().records()).extracting(ToolExecutionRecord::toolName)
                .contains("ticket.close");
    }

    @Test
    void closeTicketRequiresConfirmationTokenAndIgnoresDuplicateToken() {
        HelpdeskAgentApplicationService service = HelpdeskAgentApplicationService.seeded();
        OperatorContext operator = new OperatorContext("lead-1", "tenant-a", "support");

        ToolResult missingToken = service.closeTicket("T-1001", true, operator);
        ToolResult firstClose = service.closeTicket("T-1001", true, "confirm-close-1001", operator);
        ToolResult duplicateClose = service.closeTicket("T-1001", true, "confirm-close-1001", operator);
        ToolResult secondToken = service.closeTicket("T-1001", true, "confirm-close-1002", operator);

        assertThat(missingToken.success()).isFalse();
        assertThat(missingToken.message()).contains("confirmationToken");
        assertThat(firstClose.success()).isTrue();
        assertThat(duplicateClose.success()).isTrue();
        assertThat(duplicateClose.message()).contains("重复确认请求已忽略");
        assertThat(secondToken.success()).isFalse();
        assertThat(secondToken.message()).contains("工单已关闭");
        assertThat(service.toolLedger().records()).filteredOn(record -> record.toolName().equals("ticket.close"))
                .hasSize(4);
    }

    @Test
    void evaluatesAgentAdviceByCitationAndRequiredAction() {
        HelpdeskAgentApplicationService service = HelpdeskAgentApplicationService.seeded();
        List<AgentEvalCase> cases = List.of(new AgentEvalCase(
                "A-001",
                new AgentAdviceRequest(
                        "T-1001",
                        "客户申请退款，但订单已经发货，应该怎么处理？",
                        new OperatorContext("u1001", "tenant-a", "support")
                ),
                "refund-policy-2026",
                RequiredAction.MANUAL_REVIEW
        ));

        AgentEvalReport report = service.evaluate(cases);

        assertThat(report.caseCount()).isEqualTo(1);
        assertThat(report.citationHitRate()).isEqualByComparingTo("1.00");
        assertThat(report.actionAccuracy()).isEqualByComparingTo("1.00");
    }

    @Test
    void generatesReadableScenarioReportForEndToEndHelpdeskAgent() {
        HelpdeskAgentApplicationService service = HelpdeskAgentApplicationService.seeded();

        HelpdeskAgentScenarioReport report = service.runRefundScenario();

        assertThat(report.ticketId()).isEqualTo("T-1001");
        assertThat(report.advice()).contains("核对物流状态");
        assertThat(report.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(report.requiredAction()).isEqualTo(RequiredAction.MANUAL_REVIEW);
        assertThat(report.citationDocumentIds()).containsExactly("refund-policy-2026");
        assertThat(report.toolNames()).containsExactly("ticket.lookup", "order.lookup", "policy.search");
        assertThat(report.traceStepNames()).containsExactly(
                "ticket.lookup",
                "order.lookup",
                "policy.search",
                "advice.compose",
                "approval.plan"
        );
    }
}
