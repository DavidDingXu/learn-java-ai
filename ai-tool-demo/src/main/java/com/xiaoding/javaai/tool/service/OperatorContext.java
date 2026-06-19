package com.xiaoding.javaai.tool.service;

public record OperatorContext(String userId, String tenantId, String department) {
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
    }
}
