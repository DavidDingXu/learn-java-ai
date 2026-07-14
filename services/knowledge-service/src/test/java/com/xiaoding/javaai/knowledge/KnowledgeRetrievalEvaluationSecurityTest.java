package com.xiaoding.javaai.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.http.MediaType;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;

class KnowledgeRetrievalEvaluationSecurityTest {

    private final WebTestClient client = client();

    @Test
    void requiresTheDedicatedEvaluationScope() {
        post("eval-token").expectStatus().isOk();
        post("answer-token").expectStatus().isForbidden();
        client.post()
                .uri("/internal/v1/knowledge/retrieval/evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"退款多久到账？\",\"topK\":5}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private WebTestClient.ResponseSpec post(String token) {
        return client.post()
                .uri("/internal/v1/knowledge/retrieval/evaluations")
                .headers(headers -> headers.setBearerAuth(token))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"退款多久到账？\",\"topK\":5}")
                .exchange();
    }

    private static WebTestClient client() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("jwtDecoder", jwtDecoder());
        SecurityWebFilterChain security = new SecurityConfiguration().securityWebFilterChain(
                ServerHttpSecurity.http(),
                beans.getBeanProvider(ReactiveJwtDecoder.class),
                true,
                false
        );
        return WebTestClient.bindToController(new TestEvaluationController())
                .webFilter(new WebFilterChainProxy(security))
                .build();
    }

    private static ReactiveJwtDecoder jwtDecoder() {
        return token -> Mono.just(Jwt.withTokenValue(token)
                .header("alg", "none")
                .subject("eval-user")
                .claim("tenantId", "tenant-a")
                .claim("departmentIds", List.of("support"))
                .claim("scope", token.equals("eval-token") ? "knowledge:eval" : "knowledge:answer")
                .issuedAt(Instant.parse("2026-07-13T03:55:00Z"))
                .expiresAt(Instant.parse("2026-07-13T04:05:00Z"))
                .build());
    }

    @RestController
    @RequestMapping("/internal/v1/knowledge/retrieval/evaluations")
    static class TestEvaluationController {

        @PostMapping
        Map<String, Boolean> evaluate() {
            return Map.of("accepted", true);
        }
    }
}
