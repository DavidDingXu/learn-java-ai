package com.xiaoding.javaai.customer.identity;

public record CustomerAccessToken(String tokenValue, CustomerIdentity identity) {
    public CustomerAccessToken {
        if (tokenValue == null || tokenValue.isBlank()) {
            throw new IllegalArgumentException("tokenValue must not be blank");
        }
        if (identity == null) {
            throw new IllegalArgumentException("identity must not be null");
        }
    }
}
