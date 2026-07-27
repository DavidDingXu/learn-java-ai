package com.xiaoding.javaai.eval.agent;

import java.util.List;
import java.util.Map;

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
        Map<String, String> expectedArguments,
        Map<String, String> actualArguments,
        List<String> auditEventTypes,
        long latencyMillis,
        List<String> reasons
) {
    public AgentCaseReport {
        expectedArguments = Map.copyOf(expectedArguments == null ? Map.of() : expectedArguments);
        actualArguments = Map.copyOf(actualArguments == null ? Map.of() : actualArguments);
        auditEventTypes = List.copyOf(auditEventTypes);
        reasons = List.copyOf(reasons);
    }
}
