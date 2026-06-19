package com.xiaoding.javaai.helpdesk.agent;

import java.util.List;

public record AgentAdviceResult(
        String advice,
        RiskLevel riskLevel,
        RequiredAction requiredAction,
        List<Citation> citations,
        List<ToolExecutionRecord> toolRecords,
        AiTrace trace
) {
    public AgentAdviceResult {
        advice = advice == null ? "" : advice;
        riskLevel = riskLevel == null ? RiskLevel.LOW : riskLevel;
        requiredAction = requiredAction == null ? RequiredAction.NONE : requiredAction;
        citations = citations == null ? List.of() : List.copyOf(citations);
        toolRecords = toolRecords == null ? List.of() : List.copyOf(toolRecords);
        trace = trace == null ? new AiTrace(null, List.of()) : trace;
    }
}
