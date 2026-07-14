package com.xiaoding.javaai.knowledge.document.domain;

public record TenantId(String value) {
    public TenantId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("tenantId must not be blank");
    }
}
