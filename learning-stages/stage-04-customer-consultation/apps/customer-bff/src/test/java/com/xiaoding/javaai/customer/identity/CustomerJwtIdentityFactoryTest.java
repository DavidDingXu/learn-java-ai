package com.xiaoding.javaai.customer.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerJwtIdentityFactoryTest {

    private final CustomerJwtIdentityFactory factory = new CustomerJwtIdentityFactory();

    @Test
    void maps_customer_identity_only_from_verified_jwt_claims() {
        CustomerAccessToken token = factory.create(jwt("tenant-a"));

        assertThat(token.tokenValue()).isEqualTo("customer-token");
        assertThat(token.identity().subject()).isEqualTo("customer-42");
        assertThat(token.identity().tenantId()).isEqualTo("tenant-a");
        assertThat(token.identity().roles()).containsExactly("customer");
        assertThat(token.identity().departments()).containsExactly("support");
    }

    @Test
    void rejects_a_customer_token_without_a_tenant() {
        assertThatThrownBy(() -> factory.create(jwt(null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId");
    }

    private static Jwt jwt(String tenantId) {
        Jwt.Builder builder = Jwt.withTokenValue("customer-token")
                .header("alg", "RS256")
                .subject("customer-42")
                .claim("roles", List.of("customer"))
                .claim("departmentIds", List.of("support"))
                .issuedAt(Instant.parse("2026-07-13T03:55:00Z"))
                .expiresAt(Instant.parse("2026-07-13T04:05:00Z"));
        if (tenantId != null) builder.claim("tenantId", tenantId);
        return builder.build();
    }
}
