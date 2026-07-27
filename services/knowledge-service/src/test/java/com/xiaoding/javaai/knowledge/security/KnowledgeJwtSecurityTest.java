package com.xiaoding.javaai.knowledge.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.Citation;
import com.xiaoding.javaai.knowledge.answer.application.KnowledgeAnswer;
import com.xiaoding.javaai.knowledge.answer.application.ModelUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@ActiveProfiles("test")
@Import(KnowledgeJwtSecurityTest.StubAnswerConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeJwtSecurityTest {

    private static final String ISSUER = "https://identity.example.test";
    private static final String SECRET = "local-development-secret-must-have-at-least-32-bytes";

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("java-ai.security.jwt.enabled", () -> true);
        registry.add("java-ai.security.jwt.issuer", () -> ISSUER);
        registry.add("java-ai.security.jwt.audience", () -> "knowledge-service");
        registry.add("java-ai.security.jwt.hmac-secret", () -> SECRET);
        registry.add("java-ai.security.jwt.allowed-actors", () -> "customer-bff,ticket-agent-service");
    }

    @Autowired
    private WebTestClient client;

    @Test
    void acceptsAValidDelegatedTokenAndIgnoresIdentityHeaders() {
        post(token(TokenClaims.valid()))
                .header("X-User-Id", "attacker")
                .header("X-Tenant-Id", "other-tenant")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("secured answer");
    }

    @Test
    void acceptsTheTicketAgentAsAnExplicitlyAllowedDelegatingActor() {
        post(token(TokenClaims.valid().withActor("ticket-agent-service")))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void rejectsAnActorOutsideTheConfiguredAllowList() {
        post(token(TokenClaims.valid().withActor("unknown-service")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsCallerSuppliedIdentityInTheRequestBody() {
        client.post()
                .uri("/api/v1/knowledge/answers")
                .headers(headers -> headers.setBearerAuth(token(TokenClaims.valid())))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                        {"question":"退款什么时候到账？","tenantId":"other-tenant"}
                        """)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void rejectsInvalidSignature() {
        post(token(TokenClaims.valid(), "different-development-secret-at-least-32-bytes"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsWrongIssuer() {
        post(token(TokenClaims.valid().withIssuer("https://wrong.example.test")))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsWrongAudienceIncludingACustomerTokenUsedDirectly() {
        post(token(TokenClaims.valid()
                        .withAudience("customer-bff")
                        .withScope("customer:ask")
                        .withoutActor()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsMissingTenant() {
        post(token(TokenClaims.valid().withoutTenant()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsMissingSubject() {
        post(token(TokenClaims.valid().withoutSubject()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsMissingActor() {
        post(token(TokenClaims.valid().withoutActor()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsExpiredToken() {
        post(token(TokenClaims.valid().withExpiry(Instant.now().minusSeconds(120))))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rejectsMissingScopeAfterAuthentication() {
        post(token(TokenClaims.valid().withScope("customer:ask")))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void requires_the_write_scope_for_document_ingestion_routes() {
        client.post()
                .uri("/api/v1/knowledge/documents/refund-policy/versions/1/publish")
                .headers(headers -> headers.setBearerAuth(token(TokenClaims.valid())))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void requires_the_index_scope_for_the_manual_worker_trigger() {
        client.post()
                .uri("/internal/v1/knowledge/index-tasks/run-once")
                .headers(headers -> headers.setBearerAuth(token(TokenClaims.valid().withScope("knowledge:write"))))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void keeps_write_and_index_scopes_separate() {
        String writeToken = token(TokenClaims.valid().withScope("knowledge:write"));
        String indexToken = token(TokenClaims.valid().withScope("knowledge:index"));

        client.post()
                .uri("/api/v1/knowledge/documents/refund-policy/versions/1/publish")
                .headers(headers -> headers.setBearerAuth(writeToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isNotFound();
        client.post()
                .uri("/internal/v1/knowledge/index-tasks/run-once")
                .headers(headers -> headers.setBearerAuth(writeToken))
                .exchange()
                .expectStatus().isForbidden();

        client.post()
                .uri("/internal/v1/knowledge/index-tasks/run-once")
                .headers(headers -> headers.setBearerAuth(indexToken))
                .exchange()
                .expectStatus().isNotFound();
        client.post()
                .uri("/api/v1/knowledge/documents/refund-policy/versions/1/publish")
                .headers(headers -> headers.setBearerAuth(indexToken))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isForbidden();
    }

    private WebTestClient.RequestHeadersSpec<?> post(String token) {
        return client.post()
                .uri("/api/v1/knowledge/answers")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"退款什么时候到账？\"}");
    }

    private static String token(TokenClaims claims) {
        return token(claims, SECRET);
    }

    private static String token(TokenClaims claims, String secret) {
        Instant issuedAt = claims.expiresAt().isBefore(Instant.now())
                ? claims.expiresAt().minusSeconds(60)
                : Instant.now().minusSeconds(5);
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(claims.issuer())
                .audience(List.of(claims.audience()))
                .issuedAt(issuedAt)
                .expiresAt(claims.expiresAt())
                .claim("scope", claims.scope())
                .claim("roles", List.of("CUSTOMER"))
                .claim("departmentIds", List.of("retail"));
        if (claims.subject() != null) {
            builder.subject(claims.subject());
        }
        if (claims.tenantId() != null) {
            builder.claim("tenantId", claims.tenantId());
        }
        if (claims.actor() != null) {
            builder.claim("act", Map.of("sub", claims.actor()));
        }
        return encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), builder.build()))
                .getTokenValue();
    }

    private record TokenClaims(
            String issuer,
            String subject,
            String audience,
            String scope,
            String tenantId,
            String actor,
            Instant expiresAt
    ) {
        static TokenClaims valid() {
            return new TokenClaims(
                    ISSUER,
                    "customer-123",
                    "knowledge-service",
                    "knowledge:answer",
                    "tenant-a",
                    "customer-bff",
                    Instant.now().plusSeconds(300)
            );
        }

        TokenClaims withIssuer(String value) {
            return new TokenClaims(value, subject, audience, scope, tenantId, actor, expiresAt);
        }

        TokenClaims withAudience(String value) {
            return new TokenClaims(issuer, subject, value, scope, tenantId, actor, expiresAt);
        }

        TokenClaims withScope(String value) {
            return new TokenClaims(issuer, subject, audience, value, tenantId, actor, expiresAt);
        }

        TokenClaims withoutTenant() {
            return new TokenClaims(issuer, subject, audience, scope, null, actor, expiresAt);
        }

        TokenClaims withoutSubject() {
            return new TokenClaims(issuer, null, audience, scope, tenantId, actor, expiresAt);
        }

        TokenClaims withoutActor() {
            return new TokenClaims(issuer, subject, audience, scope, tenantId, null, expiresAt);
        }

        TokenClaims withActor(String value) {
            return new TokenClaims(issuer, subject, audience, scope, tenantId, value, expiresAt);
        }

        TokenClaims withExpiry(Instant value) {
            return new TokenClaims(issuer, subject, audience, scope, tenantId, actor, value);
        }
    }

    @TestConfiguration
    static class StubAnswerConfiguration {
        @Bean
        @Primary
        AnswerKnowledgeQuestion stubAnswerKnowledgeQuestion() {
            return command -> Mono.just(new KnowledgeAnswer(
                    "secured answer",
                    List.of(new Citation("refund-policy", "v1", "arrival-time", "退款到账时间")),
                    false,
                    null,
                    "fixture-model",
                    new ModelUsage(1, 1, 2),
                    "stop",
                    "trace-security"
            ));
        }
    }
}
