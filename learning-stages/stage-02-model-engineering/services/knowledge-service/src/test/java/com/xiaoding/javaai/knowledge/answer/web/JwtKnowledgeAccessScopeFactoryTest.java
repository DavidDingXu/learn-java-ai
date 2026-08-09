package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.security.JwtKnowledgeAccessScopeFactory;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtKnowledgeAccessScopeFactoryTest {

    @Test
    void maps_only_verified_jwt_claims_to_the_retrieval_scope() {
        Jwt jwt = Jwt.withTokenValue("delegated-token")
                .header("alg", "RS256")
                .subject("customer-42")
                .claim("tenantId", "tenant-a")
                .claim("departmentIds", List.of("support", "finance"))
                .issuedAt(Instant.parse("2026-07-13T02:00:00Z"))
                .expiresAt(Instant.parse("2026-07-13T02:10:00Z"))
                .build();

        var scope = new JwtKnowledgeAccessScopeFactory().create(jwt);

        assertThat(scope.tenantId().value()).isEqualTo("tenant-a");
        assertThat(scope.subjectId()).isEqualTo("customer-42");
        assertThat(scope.departmentIds()).containsExactly("support", "finance");
    }

    @Test
    void uses_an_empty_department_scope_when_the_claim_is_absent() {
        Jwt jwt = Jwt.withTokenValue("delegated-token")
                .header("alg", "RS256")
                .subject("customer-42")
                .claim("tenantId", "tenant-a")
                .issuedAt(Instant.parse("2026-07-13T02:00:00Z"))
                .expiresAt(Instant.parse("2026-07-13T02:10:00Z"))
                .build();

        assertThat(new JwtKnowledgeAccessScopeFactory().create(jwt).departmentIds()).isEmpty();
    }
}
