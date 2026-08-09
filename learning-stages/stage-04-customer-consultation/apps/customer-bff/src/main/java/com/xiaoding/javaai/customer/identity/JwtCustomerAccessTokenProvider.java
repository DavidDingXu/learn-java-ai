package com.xiaoding.javaai.customer.identity;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtCustomerAccessTokenProvider implements CustomerAccessTokenProvider {

    private final CustomerJwtIdentityFactory factory;

    public JwtCustomerAccessTokenProvider(CustomerJwtIdentityFactory factory) {
        this.factory = java.util.Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public CustomerAccessToken current(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalArgumentException("authenticated JWT principal is required");
        }
        return factory.create(jwt);
    }
}
