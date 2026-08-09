package com.xiaoding.javaai.ticket.security;

import org.springframework.security.core.Authentication;

@FunctionalInterface
public interface TicketIdentityProvider {
    DelegatedTicketIdentity current(Authentication authentication, String requiredActor);
}
