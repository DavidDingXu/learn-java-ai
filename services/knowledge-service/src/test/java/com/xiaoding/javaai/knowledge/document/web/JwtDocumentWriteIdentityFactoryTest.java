package com.xiaoding.javaai.knowledge.document.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JwtDocumentWriteIdentityFactoryTest {

    @Test
    void derives_tenant_and_actor_only_from_verified_jwt_claims() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("editor-42")
                .claim("tenantId", "tenant-a")
                .issuedAt(Instant.parse("2026-07-17T01:00:00Z"))
                .expiresAt(Instant.parse("2026-07-17T03:00:00Z"))
                .build();

        DocumentWriteIdentity identity = new JwtDocumentWriteIdentityFactory().create(jwt);

        assertThat(identity.tenantId().value()).isEqualTo("tenant-a");
        assertThat(identity.actorId().value()).isEqualTo("editor-42");
    }
}
