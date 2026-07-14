package com.xiaoding.javaai.labs.alibaba;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfirmationGraphTest {

    private final ConfirmationGraph graph = new ConfirmationGraph();

    @Test
    void lowRiskOperationCanExecuteDirectly() {
        assertEquals(ConfirmationStatus.EXECUTED, graph.run(RiskLevel.LOW, ApprovalDecision.NOT_REQUIRED).status());
    }

    @Test
    void highRiskOperationStopsUntilARealDecisionExists() {
        assertEquals(ConfirmationStatus.PENDING, graph.run(RiskLevel.HIGH, ApprovalDecision.MISSING).status());
        assertEquals(ConfirmationStatus.EXECUTED, graph.run(RiskLevel.HIGH, ApprovalDecision.APPROVED).status());
        assertEquals(ConfirmationStatus.REJECTED, graph.run(RiskLevel.HIGH, ApprovalDecision.REJECTED).status());
    }
}
