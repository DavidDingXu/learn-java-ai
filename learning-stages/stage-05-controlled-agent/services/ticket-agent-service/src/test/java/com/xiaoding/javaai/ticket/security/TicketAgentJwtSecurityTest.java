package com.xiaoding.javaai.ticket.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketAgentJwtSecurityTest {

    private static final String ISSUER = "https://identity.example.test";
    private static final String SECRET = "ticket-agent-development-secret-at-least-32-bytes";

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("java-ai.security.mode", () -> "jwt");
        registry.add("java-ai.security.jwt.issuer", () -> ISSUER);
        registry.add("java-ai.security.jwt.audience", () -> "ticket-agent-service");
        registry.add("java-ai.security.jwt.hmac-secret", () -> SECRET);
    }

    @Autowired
    private RestTestClient client;

    @Test
    void accepts_customer_bff_for_intake_and_jdk8_crm_for_task_read() {
        HttpHeaders createHeaders = headers(token(
                "customer-42", "customer-bff", "ticket:task:create", List.of("CUSTOMER")));
        createHeaders.set("Idempotency-Key", "handoff:tenant-a:case-1");
        AtomicReference<String> taskId = new AtomicReference<>();
        client.post()
                .uri("/api/v1/agent/tasks")
                .headers(requestHeaders -> requestHeaders.addAll(createHeaders))
                .body(Map.of(
                        "caseId", "case-1",
                        "objective", "Resolve customer consultation",
                        "businessContext", Map.of("question", "退款多久到账？")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.taskId").value(String.class, taskId::set);

        client.get()
                .uri("/api/v1/agent/tasks/{taskId}", taskId.get())
                .headers(requestHeaders -> requestHeaders.addAll(headers(token(
                        "employee-7", "jdk8-crm", "ticket:task:read",
                        List.of("TICKET_OPERATOR")))))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.taskId").isEqualTo(taskId.get());
    }

    @Test
    void rejects_task_intake_from_the_wrong_actor_as_forbidden() {
        HttpHeaders createHeaders = headers(token(
                "customer-42", "unknown-client", "ticket:task:create", List.of("CUSTOMER")));
        createHeaders.set("Idempotency-Key", "handoff:tenant-a:case-2");

        client.post()
                .uri("/api/v1/agent/tasks")
                .headers(requestHeaders -> requestHeaders.addAll(createHeaders))
                .body(Map.of(
                        "caseId", "case-2",
                        "objective", "Resolve customer consultation",
                        "businessContext", Map.of("question", "退款多久到账？")))
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo("AGENT_TASK_ACCESS_DENIED");
    }

    private static HttpHeaders headers(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static String token(String subject, String actor, String scope, List<String> roles) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(subject)
                .audience(List.of("ticket-agent-service"))
                .issuedAt(Instant.now().minusSeconds(5))
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("tenantId", "tenant-a")
                .claim("act", Map.of("sub", actor))
                .claim("scope", scope)
                .claim("roles", roles)
                .claim("departmentIds", List.of("retail"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
