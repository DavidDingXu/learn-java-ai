package com.xiaoding.javaai.agent;

import com.xiaoding.javaai.agent.service.AgentStepType;
import com.xiaoding.javaai.agent.service.TicketAgent;
import com.xiaoding.javaai.agent.service.TicketAgentRequest;
import com.xiaoding.javaai.agent.service.TicketAgentResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketAgentTest {

    @Test
    void handlesRefundTicketWithDeterministicStepsBeforeAdvice() {
        TicketAgent agent = new TicketAgent();

        TicketAgentResult result = agent.handle(new TicketAgentRequest(
                "T-1001",
                "客户申请退款，但订单已经发货，应该怎么处理？",
                "u1001",
                "tenant-a",
                "support"
        ));

        assertThat(result.advice()).contains("核对物流状态");
        assertThat(result.requiresHumanApproval()).isTrue();
        assertThat(result.steps()).extracting("type")
                .containsExactly(
                        AgentStepType.LOOKUP_TICKET,
                        AgentStepType.LOOKUP_ORDER,
                        AgentStepType.RETRIEVE_POLICY,
                        AgentStepType.ASSESS_RISK,
                        AgentStepType.COMPOSE_ADVICE
                );
    }

    @Test
    void marksHighRiskRefundAsHumanApprovalInsteadOfAutomaticAction() {
        TicketAgent agent = new TicketAgent();

        TicketAgentResult result = agent.handle(new TicketAgentRequest(
                "T-9001",
                "客户要求立刻退款 5000 元，并关闭投诉工单。",
                "u9001",
                "tenant-a",
                "support"
        ));

        assertThat(result.requiresHumanApproval()).isTrue();
        assertThat(result.advice()).contains("人工确认");
        assertThat(result.steps()).filteredOn(step -> step.type() == AgentStepType.ASSESS_RISK)
                .singleElement()
                .satisfies(step -> {
                    assertThat(step.output()).containsEntry("riskLevel", "HIGH");
                    assertThat(step.output()).containsEntry("autoExecutable", false);
                    assertThat(step.output()).containsEntry("reason", "high_value_refund_or_close_ticket");
                });
    }
}
