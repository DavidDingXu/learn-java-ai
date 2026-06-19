package com.xiaoding.javaai.observability.service;

public record QuotaPolicy(int userTokenLimit, int tenantTokenLimit) {
    public QuotaPolicy {
        if (userTokenLimit <= 0) {
            throw new IllegalArgumentException("userTokenLimit must be positive");
        }
        if (tenantTokenLimit <= 0) {
            throw new IllegalArgumentException("tenantTokenLimit must be positive");
        }
    }
}
