package com.xiaoding.javaai.ticket.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DelegatedTicketIdentityFactoryTest {

    private final DelegatedTicketIdentityFactory factory = new DelegatedTicketIdentityFactory();

    @Test
    void maps_task_owner_only_from_verified_jwt_claims() {
        Jwt jwt = jwt(Map.of(
                "tenantId", "tenant-a",
                "act", Map.of("sub", "customer-bff"),
                "roles", List.of("CUSTOMER"),
                "departmentIds", List.of("retail")
        ));

        DelegatedTicketIdentity identity = factory.create(jwt, "customer-bff");

        assertThat(identity.tenantId()).isEqualTo("tenant-a");
        assertThat(identity.subjectId()).isEqualTo("customer-42");
        assertThat(identity.actorId()).isEqualTo("customer-bff");
        assertThat(identity.roles()).containsExactly("CUSTOMER");
        assertThat(identity.departments()).containsExactly("retail");
    }

    @Test
    void rejects_a_token_delegated_by_the_wrong_actor() {
        Jwt jwt = jwt(Map.of(
                "tenantId", "tenant-a",
                "act", Map.of("sub", "unknown-client")
        ));

        assertThatThrownBy(() -> factory.create(jwt, "customer-bff"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("actor");
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token-value",
                Instant.parse("2026-07-13T00:00:00Z"),
                Instant.parse("2026-07-13T00:05:00Z"),
                Map.of("alg", "none"),
                new java.util.LinkedHashMap<>() {{
                    put("sub", "customer-42");
                    putAll(claims);
                }}
        );
    }
}
