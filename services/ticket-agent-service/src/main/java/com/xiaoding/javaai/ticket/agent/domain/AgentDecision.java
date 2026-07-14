package com.xiaoding.javaai.ticket.agent.domain;

import java.util.Map;

public sealed interface AgentDecision permits
        AgentDecision.UseTool, AgentDecision.Finish, AgentDecision.Refuse {

    record UseTool(String toolName, Map<String, String> arguments, String rationale)
            implements AgentDecision {
        public UseTool {
            toolName = requireText(toolName, "toolName");
            arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
            rationale = requireText(rationale, "rationale");
        }
    }

    record Finish(String summary) implements AgentDecision {
        public Finish {
            summary = requireText(summary, "summary");
        }
    }

    record Refuse(String reasonCode, String message) implements AgentDecision {
        public Refuse {
            reasonCode = requireText(reasonCode, "reasonCode");
            message = requireText(message, "message");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
