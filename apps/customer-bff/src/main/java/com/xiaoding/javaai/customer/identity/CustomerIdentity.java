package com.xiaoding.javaai.customer.identity;

import java.util.List;

public record CustomerIdentity(
        String subject,
        String tenantId,
        List<String> roles,
        List<String> departments
) {
    public CustomerIdentity {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        roles = roles == null ? List.of() : List.copyOf(roles);
        departments = departments == null ? List.of() : List.copyOf(departments);
    }
}
