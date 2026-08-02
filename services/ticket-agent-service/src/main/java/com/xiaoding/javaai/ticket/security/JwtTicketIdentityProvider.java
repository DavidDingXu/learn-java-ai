package com.xiaoding.javaai.ticket.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtTicketIdentityProvider implements TicketIdentityProvider {

    private final DelegatedTicketIdentityFactory factory;

    public JwtTicketIdentityProvider(DelegatedTicketIdentityFactory factory) {
        this.factory = java.util.Objects.requireNonNull(factory, "factory must not be null");
    }

    @Override
    public DelegatedTicketIdentity current(Authentication authentication, String requiredActor) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalArgumentException("authenticated JWT principal is required");
        }
        return factory.create(jwt, requiredActor);
    }
}
