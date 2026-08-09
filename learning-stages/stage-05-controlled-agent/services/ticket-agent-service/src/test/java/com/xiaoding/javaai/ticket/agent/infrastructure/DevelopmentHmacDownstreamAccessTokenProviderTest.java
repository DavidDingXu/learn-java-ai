package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DevelopmentHmacDownstreamAccessTokenProviderTest {

    private static final String SECRET = "development-downstream-secret-at-least-32-bytes";
    private static final Instant NOW = Instant.parse("2026-07-13T08:00:00Z");

    @Test
    void signs_a_short_lived_delegated_token_from_trusted_task_identity() {
        DevelopmentHmacDownstreamAccessTokenProvider provider =
                new DevelopmentHmacDownstreamAccessTokenProvider(
                        "https://identity.example.test",
                        SECRET,
                        "ticket-agent-service",
                        Duration.ofMinutes(2),
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        () -> "token-id-100");

        String token = provider.tokenFor(task(), "knowledge-service", "knowledge:answer");
        Jwt jwt = decoder().decode(token);

        assertThat(jwt.getIssuer().toString()).isEqualTo("https://identity.example.test");
        assertThat(jwt.getSubject()).isEqualTo("customer-42");
        assertThat(jwt.getAudience()).containsExactly("knowledge-service");
        assertThat(jwt.getClaimAsString("tenantId")).isEqualTo("tenant-a");
        assertThat(jwt.getClaimAsMap("act")).containsEntry("sub", "ticket-agent-service");
        assertThat(jwt.getClaimAsString("scope")).isEqualTo("knowledge:answer");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("CUSTOMER");
        assertThat(jwt.getClaimAsStringList("departmentIds")).containsExactly("retail");
        assertThat(jwt.getId()).isEqualTo("token-id-100");
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plusSeconds(120));
    }

    private static NimbusJwtDecoder decoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(
                        SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder;
    }

    private static AgentTask task() {
        return AgentTask.accepted(
                "task-100",
                new DelegatedTicketIdentity(
                        "tenant-a", "customer-42", "customer-bff",
                        List.of("CUSTOMER"), List.of("retail")),
                new AgentTaskRequest("ticket-900", "resolve", Map.of()),
                NOW);
    }
}
