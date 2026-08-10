package com.xiaoding.javaai.labs.agentscope;

import java.util.List;

public record MultiAgentResult(
        MultiAgentStatus status,
        List<SpecialistResult> specialists,
        String answer,
        String reason) {

    public MultiAgentResult {
        specialists = List.copyOf(specialists);
        if (status == MultiAgentStatus.COMPLETED && (answer == null || answer.isBlank())) {
            throw new IllegalArgumentException("completed result must contain an answer");
        }
        if (status == MultiAgentStatus.HUMAN_REQUIRED && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("human-required result must contain a reason");
        }
    }
}
