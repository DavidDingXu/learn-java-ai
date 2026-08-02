package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestionCommand;
import com.xiaoding.javaai.knowledge.answer.application.AnswerStreamEvent;
import com.xiaoding.javaai.knowledge.answer.application.Citation;
import com.xiaoding.javaai.knowledge.answer.application.KnowledgeAnswer;
import com.xiaoding.javaai.knowledge.answer.application.ModelUsage;
import com.xiaoding.javaai.knowledge.security.JwtKnowledgeAccessScopeProvider;
import com.xiaoding.javaai.knowledge.security.FixedKnowledgeAccessScopeProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnswerControllerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T03:00:00Z"), ZoneOffset.UTC
    );

    @Test
    void requestContainsOnlyTheQuestionAndUntrustedConversationContext() {
        assertThat(Arrays.stream(KnowledgeAnswerRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .toList())
                .containsExactly("question", "conversationContext");
    }

    @Test
    void rejectsABlankQuestionAtTheHttpBoundary() {
        AnswerKnowledgeQuestion useCase = command -> Mono.just(answer());
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        WebTestClient client = WebTestClient.bindToController(
                        new KnowledgeAnswerController(
                                useCase, command -> Flux.empty(), CLOCK,
                                new FixedKnowledgeAccessScopeProvider()))
                .validator(validator)
                .build();

        client.post()
                .uri("/api/v1/knowledge/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"question\":\"  \"}")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void mapsTheApplicationResultToThePublicResponse() {
        AnswerKnowledgeQuestion useCase = command -> Mono.just(answer());
        KnowledgeAnswerController controller = new KnowledgeAnswerController(
                useCase, command -> Flux.empty(), CLOCK,
                new JwtKnowledgeAccessScopeProvider()
        );

        var response = controller.answer(
                new KnowledgeAnswerRequest("退款什么时候到账？"), authentication()
        ).block();

        assertThat(response.answer()).isEqualTo("answer");
        assertThat(response.citations()).singleElement()
                .extracting(Citation::documentId)
                .isEqualTo("refund-policy");
        assertThat(response.usage().totalTokens()).isEqualTo(2);
        assertThat(response.traceId()).isEqualTo("trace-123");
    }

    @Test
    void builds_the_application_scope_from_the_authenticated_jwt() {
        AtomicReference<AnswerKnowledgeQuestionCommand> captured = new AtomicReference<>();
        KnowledgeAnswerController controller = new KnowledgeAnswerController(
                command -> {
                    captured.set(command);
                    return Mono.just(answer());
                },
                command -> Flux.empty(),
                CLOCK,
                new JwtKnowledgeAccessScopeProvider()
        );

        controller.answer(new KnowledgeAnswerRequest(
                "那银行卡会更慢吗？",
                new ConversationContextRequest(
                        "此前讨论过退款到账时间；结果：已回答",
                        List.of(
                                new ConversationTurnRequest("USER", "退款多久到账？"),
                                new ConversationTurnRequest("ASSISTANT", "请以当前政策为准")
                        )
                )), authentication()).block();

        assertThat(captured.get().accessScope().tenantId().value()).isEqualTo("tenant-a");
        assertThat(captured.get().accessScope().subjectId()).isEqualTo("customer-42");
        assertThat(captured.get().accessScope().departmentIds()).containsExactly("support");
        assertThat(captured.get().effectiveAt()).isEqualTo(Instant.parse("2026-07-13T03:00:00Z"));
        assertThat(captured.get().conversationContext().summary())
                .contains("此前讨论过退款到账时间");
        assertThat(captured.get().conversationContext().turns())
                .extracting(turn -> turn.role().name())
                .containsExactly("USER", "ASSISTANT");
    }

    @Test
    void exposesStableServerSentEventNames() {
        AnswerKnowledgeQuestion useCase = command -> Mono.just(answer());
        KnowledgeAnswerController controller = new KnowledgeAnswerController(
                useCase,
                command -> Flux.just(
                        new AnswerStreamEvent.MetadataEvent(
                                "trace-123", "knowledge-answer-v1"),
                        new AnswerStreamEvent.DeltaEvent("退款通常 1 到 5 个工作日到账。"),
                        new AnswerStreamEvent.CompletedEvent(
                                "fixture-model", new ModelUsage(1, 1, 2), "stop", 42,
                                false, null)
                ),
                CLOCK,
                new JwtKnowledgeAccessScopeProvider()
        );

        var events = controller.stream(
                new KnowledgeAnswerRequest("退款什么时候到账？"), authentication()
        ).collectList().block();

        assertThat(events).extracting(event -> event.event())
                .containsExactly("metadata", "delta", "completed");
        assertThat(events.get(2).data())
                .isEqualTo(new AnswerStreamEvent.CompletedEvent(
                        "fixture-model", new ModelUsage(1, 1, 2), "stop", 42,
                        false, null));
    }

    private static KnowledgeAnswer answer() {
        return new KnowledgeAnswer(
                "answer",
                List.of(new Citation("refund-policy", "v1", "arrival-time", "退款到账时间")),
                false,
                null,
                "fixture-model",
                new ModelUsage(1, 1, 2),
                "stop",
                "trace-123"
        );
    }

    private static Jwt delegatedJwt() {
        return Jwt.withTokenValue("delegated-token")
                .header("alg", "RS256")
                .subject("customer-42")
                .claim("tenantId", "tenant-a")
                .claim("departmentIds", List.of("support"))
                .issuedAt(Instant.parse("2026-07-13T02:55:00Z"))
                .expiresAt(Instant.parse("2026-07-13T03:05:00Z"))
                .build();
    }

    private static JwtAuthenticationToken authentication() {
        return new JwtAuthenticationToken(delegatedJwt());
    }
}
