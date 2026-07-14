package com.xiaoding.javaai.knowledge.security;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;

public final class JwtKnowledgeAccessScopeFactory {

    public KnowledgeAccessScope create(Jwt jwt) {
        return new KnowledgeAccessScope(
                new TenantId(jwt.getClaimAsString("tenantId")),
                jwt.getSubject(),
                departmentIds(jwt.getClaim("departmentIds"))
        );
    }

    private static List<String> departmentIds(Object claim) {
        if (claim == null) return List.of();
        if (!(claim instanceof Collection<?> values)) {
            throw new IllegalArgumentException("departmentIds claim must be an array");
        }
        return values.stream().map(String.class::cast).toList();
    }
}
