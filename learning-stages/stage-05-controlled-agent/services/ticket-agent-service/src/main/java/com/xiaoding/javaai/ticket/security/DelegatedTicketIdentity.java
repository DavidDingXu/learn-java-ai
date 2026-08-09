package com.xiaoding.javaai.ticket.security;

import java.util.List;

public record DelegatedTicketIdentity(
        String tenantId,
        String subjectId,
        String actorId,
        List<String> roles,
        List<String> departments
) {
    public DelegatedTicketIdentity {
        tenantId = requireText(tenantId, "tenantId");
        subjectId = requireText(subjectId, "subjectId");
        actorId = requireText(actorId, "actorId");
        roles = roles == null ? List.of() : List.copyOf(roles);
        departments = departments == null ? List.of() : List.copyOf(departments);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
