package com.xiaoding.javaai.eval.agent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AgentEvalCase(
        String id,
        String objective,
        Map<String, String> businessContext,
        String expectedState,
        String expectedTool,
        String expectedRisk,
        String expectedRole,
        List<String> forbiddenAuditEvents,
        List<String> forbiddenAuditFragments
) {
    public AgentEvalCase {
        id = requireText(id, "id");
        objective = requireText(objective, "objective");
        businessContext = Map.copyOf(new LinkedHashMap<>(
                businessContext == null ? Map.of() : businessContext));
        expectedState = requireText(expectedState, "expectedState");
        forbiddenAuditEvents = List.copyOf(
                forbiddenAuditEvents == null ? List.of() : forbiddenAuditEvents);
        forbiddenAuditFragments = List.copyOf(
                forbiddenAuditFragments == null ? List.of() : forbiddenAuditFragments);
    }

    public AgentEvalCase(
            String id,
            String objective,
            Map<String, String> businessContext,
            String expectedState,
            String expectedTool,
            String expectedRisk,
            String expectedRole,
            List<String> forbiddenAuditEvents
    ) {
        this(id, objective, businessContext, expectedState, expectedTool, expectedRisk,
                expectedRole, forbiddenAuditEvents, List.of());
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
