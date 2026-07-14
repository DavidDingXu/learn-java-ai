package com.xiaoding.javaai.customer.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import reactor.test.StepVerifier;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LocalHmacDelegatedTokenClientTest {

    private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";

    @Test
    void preservesCustomerIdentityAndAddsAudienceScopeAndActor() {
        LocalHmacDelegatedTokenClient client = new LocalHmacDelegatedTokenClient(
                SECRET,
                "https://identity.example.test",
                "knowledge-service",
                "customer-bff",
                Duration.ofMinutes(5)
        );
        CustomerIdentity identity = new CustomerIdentity(
                "customer-123",
                "tenant-a",
                List.of("CUSTOMER"),
                List.of("retail")
        );

        StepVerifier.create(client.exchange(new CustomerAccessToken("customer-token", identity)))
                .assertNext(delegated -> {
                    Jwt jwt = decoder().decode(delegated.tokenValue());
                    assertThat(jwt.getSubject()).isEqualTo("customer-123");
                    assertThat(jwt.getAudience()).containsExactly("knowledge-service");
                    assertThat(jwt.getClaimAsString("tenantId")).isEqualTo("tenant-a");
                    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
                    assertThat(jwt.getClaimAsStringList("departmentIds")).containsExactly("retail");
                    assertThat(jwt.getClaimAsString("scope")).isEqualTo("knowledge:answer");
                    assertThat(jwt.<Map<String, Object>>getClaim("act"))
                            .containsEntry("sub", "customer-bff");
                    assertThat(delegated.expiresAt()).isAfter(Instant.now());
                })
                .verifyComplete();
    }

    private static NimbusJwtDecoder decoder() {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
