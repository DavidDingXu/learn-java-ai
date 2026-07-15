package com.xiaoding.javaai.knowledge.answer.application;

import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerStreamModel;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class StreamingKnowledgeAnswerServiceTest {

    @Test
    void passes_the_same_trusted_scope_to_streaming_context_retrieval() {
        AtomicReference<PolicyContextQuery> captured = new AtomicReference<>();
        PolicyContextSource source = query -> {
            captured.set(query);
            return Mono.just(List.of());
        };
        StreamingKnowledgeAnswerService service = new StreamingKnowledgeAnswerService(
                source,
                prompt -> Flux.just(new ModelStreamChunk(
                        "无法确认。", "fixture-model", new ModelUsage(1, 1, 2), "stop")),
                () -> "trace-123",
                "knowledge-answer-v1",
                "system instruction"
        );
        KnowledgeAccessScope scope = new KnowledgeAccessScope(
                new TenantId("tenant-a"), "customer-42", List.of("support")
        );
        Instant effectiveAt = Instant.parse("2026-07-13T03:00:00Z");

        service.stream(new AnswerKnowledgeQuestionCommand("退款多久到账？", scope, effectiveAt))
                .collectList()
                .block();

        org.assertj.core.api.Assertions.assertThat(captured.get().accessScope()).isSameAs(scope);
        org.assertj.core.api.Assertions.assertThat(captured.get().effectiveAt()).isEqualTo(effectiveAt);
    }

    @Test
    void emitsAStableBusinessEventSequence() {
        PolicyContext context = new PolicyContext(
                "refund-policy", "v1", "arrival-time", "退款到账时间", "1 到 5 个工作日到账。"
        );
        PolicyContextSource source = query -> Mono.just(List.of(context));
        KnowledgeAnswerStreamModel model = prompt -> Flux.just(
                new ModelStreamChunk("退款审核通过后，", "fixture-model", null, null),
                new ModelStreamChunk("通常 1 到 5 个工作日到账。", "fixture-model", new ModelUsage(10, 8, 18), "stop")
        );
        StreamingKnowledgeAnswerService service = new StreamingKnowledgeAnswerService(
                source,
                model,
                () -> "trace-123",
                "knowledge-answer-v1",
                "system instruction"
        );

        StepVerifier.create(service.stream(command("退款什么时候到账？")))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isInstanceOf(AnswerStreamEvent.MetadataEvent.class))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isEqualTo(new AnswerStreamEvent.DeltaEvent("退款审核通过后，")))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isEqualTo(new AnswerStreamEvent.DeltaEvent("通常 1 到 5 个工作日到账。")))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isEqualTo(new AnswerStreamEvent.CitationEvent(
                                new Citation("refund-policy", "v1", "arrival-time", "退款到账时间"))))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isInstanceOf(AnswerStreamEvent.CompletedEvent.class))
                .verifyComplete();
    }

    @Test
    void endsWithAnErrorWhenTheProviderDoesNotReturnUsageMetadata() {
        KnowledgeAnswerStreamModel model = prompt -> Flux.just(
                new ModelStreamChunk("退款通常需要几个工作日。", "fixture-model", null, "stop")
        );
        StreamingKnowledgeAnswerService service = new StreamingKnowledgeAnswerService(
                query -> Mono.just(List.of()),
                model,
                () -> "trace-123",
                "knowledge-answer-v1",
                "system instruction"
        );

        StepVerifier.create(service.stream(command("退款什么时候到账？")))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isInstanceOf(AnswerStreamEvent.MetadataEvent.class))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isEqualTo(new AnswerStreamEvent.DeltaEvent("退款通常需要几个工作日。")))
                .assertNext(event -> org.assertj.core.api.Assertions.assertThat(event)
                        .isEqualTo(new AnswerStreamEvent.ErrorEvent(
                                "MODEL_STREAM_FAILED", "The model stream ended unexpectedly")))
                .verifyComplete();
    }

    @Test
    void cancellingTheSubscriberCancelsTheModelStream() {
        AtomicBoolean cancelled = new AtomicBoolean();
        KnowledgeAnswerStreamModel model = prompt -> Flux.concat(
                        Flux.just(new ModelStreamChunk("首个片段", "fixture-model", null, null)),
                        Flux.<ModelStreamChunk>never())
                .doOnCancel(() -> cancelled.set(true));
        StreamingKnowledgeAnswerService service = new StreamingKnowledgeAnswerService(
                query -> Mono.just(List.of()),
                model,
                () -> "trace-123",
                "knowledge-answer-v1",
                "system instruction"
        );

        StepVerifier.create(service.stream(command("问题")))
                .expectNextCount(2)
                .thenCancel()
                .verify();

        org.assertj.core.api.Assertions.assertThat(cancelled).isTrue();
    }

    private static AnswerKnowledgeQuestionCommand command(String question) {
        return new AnswerKnowledgeQuestionCommand(
                question,
                new KnowledgeAccessScope(new TenantId("tenant-test"), "user-test", List.of()),
                Instant.parse("2026-07-13T03:00:00Z")
        );
    }
}
