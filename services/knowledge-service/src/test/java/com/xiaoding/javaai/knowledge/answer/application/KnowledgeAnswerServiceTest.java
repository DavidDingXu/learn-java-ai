package com.xiaoding.javaai.knowledge.answer.application;

import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerModel;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerTelemetry;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeAnswerServiceTest {

    @Test
    void passes_the_trusted_scope_and_effective_time_to_context_retrieval() {
        AtomicReference<PolicyContextQuery> captured = new AtomicReference<>();
        PolicyContextSource contextSource = query -> {
            captured.set(query);
            return Mono.just(List.of());
        };
        KnowledgeAnswerModel model = prompt -> Mono.just(new ModelAnswerDraft(
                "当前知识范围内无法确认。",
                List.of(),
                true,
                "没有检索到可引用制度",
                "fixture-model",
                new ModelUsage(10, 8, 18),
                "stop"
        ));
        KnowledgeAnswerService service = new KnowledgeAnswerService(
                contextSource,
                model,
                () -> "trace-123",
                passthroughTelemetry(),
                ExecutionMode.PROVIDER_PROTOCOL_FIXTURE,
                "knowledge-answer-v1",
                "system instruction"
        );
        KnowledgeAccessScope scope = new KnowledgeAccessScope(
                new TenantId("tenant-a"), "customer-42", List.of("support")
        );
        Instant effectiveAt = Instant.parse("2026-07-13T03:00:00Z");

        service.answer(new AnswerKnowledgeQuestionCommand("退款多久到账？", scope, effectiveAt)).block();

        org.assertj.core.api.Assertions.assertThat(captured.get().accessScope()).isSameAs(scope);
        org.assertj.core.api.Assertions.assertThat(captured.get().effectiveAt()).isEqualTo(effectiveAt);
    }

    @Test
    void answersWithModelMetadataAndCitationsFromTheLoadedContext() {
        PolicyContext context = new PolicyContext(
                "refund-policy",
                "v1",
                "arrival-time",
                "退款到账时间",
                "审核通过后，原路退款通常在 1 到 5 个工作日到账。"
        );
        PolicyContextSource contextSource = query -> Mono.just(List.of(context));
        KnowledgeAnswerModel model = prompt -> Mono.just(new ModelAnswerDraft(
                "退款审核通过后，通常还需要 1 到 5 个工作日原路到账。",
                List.of("arrival-time"),
                false,
                null,
                "fixture-model",
                new ModelUsage(42, 18, 60),
                "stop"
        ));

        KnowledgeAnswerService service = new KnowledgeAnswerService(
                contextSource,
                model,
                () -> "trace-123",
                passthroughTelemetry(),
                ExecutionMode.PROVIDER_PROTOCOL_FIXTURE,
                "knowledge-answer-v1",
                "system instruction"
        );

        StepVerifier.create(service.answer(command("为什么退款还没到账？")))
                .assertNext(answer -> {
                    org.assertj.core.api.Assertions.assertThat(answer.answer()).contains("1 到 5 个工作日");
                    org.assertj.core.api.Assertions.assertThat(answer.model()).isEqualTo("fixture-model");
                    org.assertj.core.api.Assertions.assertThat(answer.usage().totalTokens()).isEqualTo(60);
                    org.assertj.core.api.Assertions.assertThat(answer.finishReason()).isEqualTo("stop");
                    org.assertj.core.api.Assertions.assertThat(answer.traceId()).isEqualTo("trace-123");
                    org.assertj.core.api.Assertions.assertThat(answer.executionMode())
                            .isEqualTo(ExecutionMode.PROVIDER_PROTOCOL_FIXTURE);
                    org.assertj.core.api.Assertions.assertThat(answer.citations())
                            .containsExactly(new Citation(
                                    "refund-policy", "v1", "arrival-time", "退款到账时间"));
                })
                .verifyComplete();
    }

    @Test
    void rejectsCitationsThatWereNotPresentInTheTrustedContext() {
        PolicyContext context = new PolicyContext(
                "refund-policy", "v1", "arrival-time", "退款到账时间", "1 到 5 个工作日到账。"
        );
        KnowledgeAnswerModel model = prompt -> Mono.just(new ModelAnswerDraft(
                "已经为你完成退款。",
                List.of("non-existent-section"),
                false,
                null,
                "fixture-model",
                new ModelUsage(10, 10, 20),
                "stop"
        ));
        KnowledgeAnswerService service = new KnowledgeAnswerService(
                query -> Mono.just(List.of(context)),
                model,
                () -> "trace-123",
                passthroughTelemetry(),
                ExecutionMode.PROVIDER_PROTOCOL_FIXTURE,
                "knowledge-answer-v1",
                "system instruction"
        );

        StepVerifier.create(service.answer(command("退款怎么样了？")))
                .expectErrorMatches(error -> error instanceof InvalidModelAnswerException
                        && error.getMessage().contains("unknown citation"))
                .verify();
    }

    @Test
    void rejectsBlankQuestionsBeforeLoadingContextOrCallingTheModel() {
        assertThatThrownBy(() -> command("  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("question");
    }

    private static AnswerKnowledgeQuestionCommand command(String question) {
        return new AnswerKnowledgeQuestionCommand(
                question,
                new KnowledgeAccessScope(new TenantId("tenant-test"), "user-test", List.of()),
                Instant.parse("2026-07-13T03:00:00Z")
        );
    }

    private static KnowledgeAnswerTelemetry passthroughTelemetry() {
        return new KnowledgeAnswerTelemetry() {
            @Override
            public <T> Mono<T> observe(KnowledgeOperation operation, Supplier<Mono<T>> publisher) {
                return publisher.get();
            }
        };
    }
}
