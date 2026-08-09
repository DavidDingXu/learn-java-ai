package com.xiaoding.javaai.ticket.agent.application;

import java.util.List;

public record ConfirmationActor(
        String tenantId,
        String subjectId,
        String actorId,
        List<String> roles
) {
    public ConfirmationActor {
        tenantId = requireText(tenantId, "tenantId");
        subjectId = requireText(subjectId, "subjectId");
        actorId = requireText(actorId, "actorId");
        roles = roles == null ? List.of() : List.copyOf(roles);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
