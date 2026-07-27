package com.xiaoding.javaai.labs.alibaba;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfirmationGraphTest {

    private final ConfirmationGraph graph = new ConfirmationGraph();

    @Test
    void lowRiskOperationCanExecuteDirectly() {
        ConfirmationResult result = graph.run(RiskLevel.LOW, ApprovalDecision.NOT_REQUIRED);

        assertEquals(ConfirmationStatus.EXECUTED, result.status());
        assertEquals(List.of("classify", "execute"), result.visitedNodes());
    }

    @Test
    void highRiskOperationStopsUntilARealDecisionExists() {
        ConfirmationResult missing = graph.run(RiskLevel.HIGH, ApprovalDecision.MISSING);
        ConfirmationResult approved = graph.run(RiskLevel.HIGH, ApprovalDecision.APPROVED);
        ConfirmationResult rejected = graph.run(RiskLevel.HIGH, ApprovalDecision.REJECTED);

        assertEquals(ConfirmationStatus.PENDING, missing.status());
        assertEquals(List.of("classify", "confirm", "pending"), missing.visitedNodes());
        assertEquals(ConfirmationStatus.EXECUTED, approved.status());
        assertEquals(List.of("classify", "confirm", "execute"), approved.visitedNodes());
        assertEquals(ConfirmationStatus.REJECTED, rejected.status());
        assertEquals(List.of("classify", "confirm", "reject"), rejected.visitedNodes());
    }
}
