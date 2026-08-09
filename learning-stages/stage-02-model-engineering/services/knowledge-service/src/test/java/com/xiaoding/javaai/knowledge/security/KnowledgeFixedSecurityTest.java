package com.xiaoding.javaai.knowledge.security;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "java-ai.security.mode=fixed"
})
@Import(KnowledgeFixedSecurityTest.StubAnswerConfiguration.class)
@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class KnowledgeFixedSecurityTest {

    @Autowired
    private WebTestClient client;

    @Autowired
    private AtomicReference<KnowledgeAccessScopeSnapshot> capturedScope;

    @Test
    void usesTheFixedLocalIdentityWithoutRequiringAToken() {
        client.post()
                .uri("/api/v1/knowledge/answers")
                .header("X-Tenant-Id", "attacker-tenant")
                .header("X-User-Id", "attacker-user")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"退款什么时候到账？\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.answer").isEqualTo("fixed identity answer");

        assertThat(capturedScope.get()).isEqualTo(new KnowledgeAccessScopeSnapshot(
                "tenant-a", "local-user", List.of("support")
        ));
    }

    record KnowledgeAccessScopeSnapshot(String tenantId, String subjectId, List<String> departmentIds) {
    }

    @TestConfiguration
    static class StubAnswerConfiguration {

        @Bean
        AtomicReference<KnowledgeAccessScopeSnapshot> capturedScope() {
            return new AtomicReference<>();
        }

        @Bean
        @Primary
        AnswerKnowledgeQuestion stubAnswerKnowledgeQuestion(
                AtomicReference<KnowledgeAccessScopeSnapshot> capturedScope
        ) {
            return command -> {
                var scope = command.accessScope();
                capturedScope.set(new KnowledgeAccessScopeSnapshot(
                        scope.tenantId().value(), scope.subjectId(), scope.departmentIds()
                ));
                return Mono.just(new KnowledgeAnswer(
                        "fixed identity answer",
                        List.of(new Citation("refund-policy", "v1", "arrival-time", "退款到账时间")),
                        false,
                        null,
                        "fixture-model",
                        new ModelUsage(1, 1, 2),
                        "stop",
                        "trace-fixed-security"
                ));
            };
        }
    }
}
