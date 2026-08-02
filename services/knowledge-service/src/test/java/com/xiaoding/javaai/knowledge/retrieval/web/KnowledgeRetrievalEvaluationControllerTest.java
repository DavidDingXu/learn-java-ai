package com.xiaoding.javaai.knowledge.retrieval.web;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeRetrievalResult;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrieveKnowledgeQuery;
import com.xiaoding.javaai.knowledge.retrieval.application.RetrievedKnowledgeChunk;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeRetriever;
import com.xiaoding.javaai.knowledge.security.JwtKnowledgeAccessScopeProvider;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalEvaluationControllerTest {

    @Test
    void rejectsAnOversizedEvaluationQuestionBeforeRetrieval() {
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            var violations = validatorFactory.getValidator().validate(
                    new KnowledgeRetrievalEvaluationRequest("x".repeat(2001), 5)
            );

            assertThat(violations)
                    .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString())
                            .isEqualTo("question"));
        }
    }

    @Test
    void evaluatesRetrievalWithScopeFromTheDelegatedToken() {
        AtomicReference<RetrieveKnowledgeQuery> captured = new AtomicReference<>();
        KnowledgeRetriever retriever = query -> {
            captured.set(query);
            return new KnowledgeRetrievalResult("embedding-v1", List.of(new RetrievedKnowledgeChunk(
                    "chunk-arrival",
                    new DocumentId("refund-policy"),
                    2,
                    List.of("售后政策", "到账时间"),
                    null,
                    "银行卡通常需要一到五个工作日。",
                    0.92
            )));
        };
        Clock clock = Clock.fixed(Instant.parse("2026-07-13T04:00:00Z"), ZoneOffset.UTC);
        KnowledgeRetrievalEvaluationController controller =
                new KnowledgeRetrievalEvaluationController(
                        retriever, clock, new JwtKnowledgeAccessScopeProvider());

        KnowledgeRetrievalEvaluationResponse response = controller.evaluate(
                new KnowledgeRetrievalEvaluationRequest("退款多久到账？", 5),
                new JwtAuthenticationToken(delegatedJwt())
        ).block();

        assertThat(response.embeddingModel()).isEqualTo("embedding-v1");
        assertThat(response.chunkIds()).containsExactly("chunk-arrival");
        assertThat(captured.get().topK()).isEqualTo(5);
        assertThat(captured.get().effectiveAt()).isEqualTo(Instant.parse("2026-07-13T04:00:00Z"));
        assertThat(captured.get().accessScope().tenantId().value()).isEqualTo("tenant-a");
        assertThat(captured.get().accessScope().subjectId()).isEqualTo("eval-user");
        assertThat(captured.get().accessScope().departmentIds()).containsExactly("support");
    }

    private static Jwt delegatedJwt() {
        return Jwt.withTokenValue("delegated-token")
                .header("alg", "RS256")
                .subject("eval-user")
                .claim("tenantId", "tenant-a")
                .claim("departmentIds", List.of("support"))
                .issuedAt(Instant.parse("2026-07-13T03:55:00Z"))
                .expiresAt(Instant.parse("2026-07-13T04:05:00Z"))
                .build();
    }
}
