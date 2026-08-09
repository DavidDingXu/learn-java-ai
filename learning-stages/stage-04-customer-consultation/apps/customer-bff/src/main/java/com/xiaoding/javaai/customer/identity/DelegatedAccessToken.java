package com.xiaoding.javaai.customer.identity;

import java.time.Instant;

public record DelegatedAccessToken(String tokenValue, Instant expiresAt) {
    public DelegatedAccessToken {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("tokenValue must not be blank");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt must not be null");
        }
    }
}
