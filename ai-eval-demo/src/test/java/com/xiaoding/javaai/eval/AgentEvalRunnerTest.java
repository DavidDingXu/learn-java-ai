package com.xiaoding.javaai.eval;

import com.xiaoding.javaai.eval.service.AgentEvalCase;
import com.xiaoding.javaai.eval.service.AgentEvalObservation;
import com.xiaoding.javaai.eval.service.AgentEvalReport;
import com.xiaoding.javaai.eval.service.AgentEvalRunner;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentEvalRunnerTest {

    private final AgentEvalRunner runner = new AgentEvalRunner();

    @Test
    void passesWhenToolPathMatchesExpectedOrder() {
        AgentEvalCase evalCase = new AgentEvalCase(
                "agent-1",
                "客户申请退款但订单已发货怎么办",
                List.of("ticket.lookup", "order.lookup", "policy.search", "advice.compose"),
                false
        );
        AgentEvalObservation observation = new AgentEvalObservation(
                "agent-1",
                List.of("ticket.lookup", "order.lookup", "policy.search", "advice.compose"),
                false,
                "MEDIUM"
        );

        AgentEvalReport report = runner.run(List.of(evalCase), List.of(observation));

        assertThat(report.total()).isEqualTo(1);
        assertThat(report.pathPassed()).isEqualTo(1);
        assertThat(report.riskPassed()).isEqualTo(1);
        assertThat(report.results().getFirst().passed()).isTrue();
    }

    @Test
    void failsWhenAgentSkipsRequiredTool() {
        AgentEvalCase evalCase = new AgentEvalCase(
                "agent-2",
                "高金额退款能不能直接处理",
                List.of("ticket.lookup", "order.lookup", "policy.search", "risk.review", "advice.compose"),
                true
        );
        AgentEvalObservation observation = new AgentEvalObservation(
                "agent-2",
                List.of("ticket.lookup", "policy.search", "advice.compose"),
                true,
                "HIGH"
        );

        AgentEvalReport report = runner.run(List.of(evalCase), List.of(observation));

        assertThat(report.pathPassed()).isZero();
        assertThat(report.results().getFirst().passed()).isFalse();
        assertThat(report.results().getFirst().reason()).contains("tool_path_miss");
    }

    @Test
    void failsWhenHumanApprovalExpectationIsWrong() {
        AgentEvalCase evalCase = new AgentEvalCase(
                "agent-3",
                "高金额退款是否需要人工复核",
                List.of("ticket.lookup", "policy.search", "risk.review", "advice.compose"),
                true
        );
        AgentEvalObservation observation = new AgentEvalObservation(
                "agent-3",
                List.of("ticket.lookup", "policy.search", "risk.review", "advice.compose"),
                false,
                "HIGH"
        );

        AgentEvalReport report = runner.run(List.of(evalCase), List.of(observation));

        assertThat(report.pathPassed()).isEqualTo(1);
        assertThat(report.approvalPassed()).isZero();
        assertThat(report.results().getFirst().passed()).isFalse();
        assertThat(report.results().getFirst().reason()).contains("approval_miss");
    }
}
