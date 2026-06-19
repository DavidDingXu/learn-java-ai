package com.xiaoding.javaai.agent.service;

import java.util.List;

public record TicketAgentResult(String advice, boolean requiresHumanApproval, List<AgentStep> steps) {
    public TicketAgentResult {
        advice = advice == null ? "" : advice;
        steps = steps == null ? List.of() : List.copyOf(steps);
    }
}
