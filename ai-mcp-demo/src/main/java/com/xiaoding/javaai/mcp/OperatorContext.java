package com.xiaoding.javaai.mcp;

import java.util.Set;

public record OperatorContext(String userId, String tenantId, String department, Set<String> permissions) {
    public OperatorContext(String userId, String tenantId, String department) {
        this(userId, tenantId, department, Set.of());
    }

    public OperatorContext {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("department must not be blank");
        }
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}
