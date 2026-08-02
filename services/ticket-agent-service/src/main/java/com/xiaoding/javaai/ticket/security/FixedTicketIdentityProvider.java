package com.xiaoding.javaai.ticket.security;

import org.springframework.security.core.Authentication;

import java.util.List;

public final class FixedTicketIdentityProvider implements TicketIdentityProvider {

    private final String tenantId;
    private final String subjectId;
    private final List<String> roles;
    private final List<String> departments;

    public FixedTicketIdentityProvider(
            String tenantId,
            String subjectId,
            List<String> roles,
            List<String> departments
    ) {
        this.tenantId = requireText(tenantId, "tenantId");
        this.subjectId = requireText(subjectId, "subjectId");
        this.roles = roles == null ? List.of() : List.copyOf(roles);
        this.departments = departments == null ? List.of() : List.copyOf(departments);
    }

    @Override
    public DelegatedTicketIdentity current(Authentication authentication, String requiredActor) {
        return new DelegatedTicketIdentity(
                tenantId, subjectId, requireText(requiredActor, "requiredActor"), roles, departments);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
