package com.xiaoding.javaai.helpdesk.agent;

import java.util.List;

public record HelpdeskAgentScenarioReport(
        String ticketId,
        String advice,
        RiskLevel riskLevel,
        RequiredAction requiredAction,
        List<String> citationDocumentIds,
        List<String> toolNames,
        List<String> traceStepNames
) {
    public HelpdeskAgentScenarioReport {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId must not be blank");
        }
        advice = advice == null ? "" : advice;
        riskLevel = riskLevel == null ? RiskLevel.LOW : riskLevel;
        requiredAction = requiredAction == null ? RequiredAction.NONE : requiredAction;
        citationDocumentIds = citationDocumentIds == null ? List.of() : List.copyOf(citationDocumentIds);
        toolNames = toolNames == null ? List.of() : List.copyOf(toolNames);
        traceStepNames = traceStepNames == null ? List.of() : List.copyOf(traceStepNames);
    }
}
