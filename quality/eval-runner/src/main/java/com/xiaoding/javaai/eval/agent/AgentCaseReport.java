package com.xiaoding.javaai.eval.agent;

import java.util.List;

public record AgentCaseReport(
        String caseId,
        boolean passed,
        String expectedState,
        String actualState,
        String expectedTool,
        String actualTool,
        String expectedRisk,
        String actualRisk,
        String expectedRole,
        String actualRole,
        List<String> auditEventTypes,
        long latencyMillis,
        List<String> reasons
) {
    public AgentCaseReport {
        auditEventTypes = List.copyOf(auditEventTypes);
        reasons = List.copyOf(reasons);
    }
}
