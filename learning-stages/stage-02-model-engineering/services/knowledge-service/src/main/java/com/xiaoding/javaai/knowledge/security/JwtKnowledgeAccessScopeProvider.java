package com.xiaoding.javaai.knowledge.security;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

public final class JwtKnowledgeAccessScopeProvider implements KnowledgeAccessScopeProvider {

    private final JwtKnowledgeAccessScopeFactory factory = new JwtKnowledgeAccessScopeFactory();

    @Override
    public KnowledgeAccessScope currentScope(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("An authenticated JWT principal is required");
        }
        return factory.create(jwt);
    }
}
