package com.xiaoding.javaai.customer.consultation.application;

import com.xiaoding.javaai.customer.consultation.application.port.ConsultationRateLimiter;
import com.xiaoding.javaai.customer.consultation.application.port.IdGenerator;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerStreamClient;
import com.xiaoding.javaai.customer.consultation.application.port.TicketTaskClient;
import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.ConversationWindowPolicy;
import com.xiaoding.javaai.customer.consultation.domain.FeedbackRating;
import com.xiaoding.javaai.customer.consultation.domain.KnowledgeAnswerView;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.consultation.infrastructure.InMemoryConsultationSessionStore;
import com.xiaoding.javaai.customer.identity.CustomerAccessToken;
import com.xiaoding.javaai.customer.identity.CustomerIdentity;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import com.xiaoding.javaai.customer.identity.DelegatedTokenClient;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerConsultationServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-13T04:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void answers_with_a_delegated_token_and_only_the_owned_conversation_context() {
        AtomicReference<KnowledgeAnswerClient.Request> captured = new AtomicReference<>();
        KnowledgeAnswerClient knowledge = (token, request) -> {
            captured.set(request);
            return Mono.just(answer());
        };
        CustomerConsultationService service = service(knowledge, acceptingTicketClient());

        CustomerAnswer first = service.answer(customer(),
                new AnswerCustomerQuestion(null, "退款多久到账？")).block();
        CustomerAnswer second = service.answer(customer(),
                new AnswerCustomerQuestion(first.conversationId(), "那银行卡会更慢吗？")).block();

        assertThat(second.conversationId()).isEqualTo(first.conversationId());
        assertThat(second.attemptId()).isNotEqualTo(first.attemptId());
        assertThat(captured.get().question()).isEqualTo("那银行卡会更慢吗？");
        assertThat(captured.get().context().turns())
                .extracting(turn -> turn.role().name())
                .containsExactly("USER", "ASSISTANT");
    }

    @Test
    void keeps_retry_feedback_and_handoff_linked_to_the_original_answer_attempt() {
        AtomicReference<String> idempotencyKey = new AtomicReference<>();
        TicketTaskClient ticket = (token, key, snapshot) -> {
            idempotencyKey.set(key);
            return Mono.just(new TicketHandoffReceipt("task-100", "ACCEPTED", false));
        };
        CustomerConsultationService service = service((token, request) -> Mono.just(answer()), ticket);
        CustomerAnswer first = service.answer(customer(),
                new AnswerCustomerQuestion(null, "退款多久到账？")).block();

        service.recordFeedback(customer(), new RecordAnswerFeedback(
                first.conversationId(), first.attemptId(), FeedbackRating.NOT_HELPFUL,
                "ANSWER_INCOMPLETE", "没有说明银行卡差异")).block();
        CustomerAnswer retry = service.retry(customer(), new RetryCustomerAnswer(
                first.conversationId(), first.attemptId())).block();
        TicketHandoffReceipt receipt = service.handoff(customer(), new HandoffConsultation(
                first.conversationId(), retry.attemptId(), "CUSTOMER_REQUESTED_HUMAN")).block();

        assertThat(retry.retryOfAttemptId()).isEqualTo(first.attemptId());
        assertThat(receipt.taskId()).isEqualTo("task-100");
        assertThat(idempotencyKey.get())
                .matches("handoff:v1:[0-9a-f]{64}")
                .doesNotContain("tenant-a", first.conversationId(), retry.attemptId());
    }

    @Test
    void refuses_to_load_another_customers_conversation() {
        CustomerConsultationService service = service(
                (token, request) -> Mono.just(answer()), acceptingTicketClient());
        CustomerAnswer answer = service.answer(customer(),
                new AnswerCustomerQuestion(null, "退款多久到账？")).block();
        CustomerAccessToken other = new CustomerAccessToken("other-token",
                new CustomerIdentity("customer-99", "tenant-a", List.of("customer"), List.of()));

        assertThatThrownBy(() -> service.answer(other,
                new AnswerCustomerQuestion(answer.conversationId(), "继续")).block())
                .isInstanceOf(ConversationAccessDeniedException.class);
    }

    @Test
    void completes_a_stream_attempt_only_after_the_completed_event() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("conversation-1", "attempt-1"));
        IdGenerator idGenerator = ids::removeFirst;
        DelegatedTokenClient tokenClient = source -> Mono.just(
                new DelegatedAccessToken("delegated-token", NOW.plusSeconds(300)));
        InMemoryConsultationSessionStore store = new InMemoryConsultationSessionStore();
        CustomerConsultationService service = new CustomerConsultationService(
                store,
                (token, request) -> Mono.just(answer()),
                (token, request) -> Flux.just(
                        new KnowledgeAnswerStreamClient.Metadata("trace-123"),
                        new KnowledgeAnswerStreamClient.Delta("退款通常在 1 到 5 个工作日到账。"),
                        new KnowledgeAnswerStreamClient.Citation(
                                new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                        new KnowledgeAnswerStreamClient.Completed(false, null)),
                acceptingTicketClient(), tokenClient, tokenClient,
                (identity, now) -> true,
                new ConversationWindowPolicy(8, 800, 500), idGenerator,
                CLOCK, Duration.ofMinutes(30)
        );

        StepVerifier.create(service.stream(customer(),
                        new AnswerCustomerQuestion(null, "退款多久到账？")))
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.SessionStarted)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Metadata)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Delta)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Citation)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Completed)
                .verifyComplete();

        assertThat(store.findById("conversation-1").block()
                .requireAttempt("attempt-1").status().name()).isEqualTo("COMPLETED");
    }

    @Test
    void persistsAStreamRefusalBeforePublishingTheCompletedEvent() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("conversation-1", "attempt-1"));
        DelegatedTokenClient tokenClient = source -> Mono.just(
                new DelegatedAccessToken("delegated-token", NOW.plusSeconds(300)));
        InMemoryConsultationSessionStore store = new InMemoryConsultationSessionStore();
        CustomerConsultationService service = new CustomerConsultationService(
                store,
                (token, request) -> Mono.just(answer()),
                (token, request) -> Flux.just(
                        new KnowledgeAnswerStreamClient.Metadata("trace-123"),
                        new KnowledgeAnswerStreamClient.Delta(
                                "当前资料无法确认这笔退款的审核状态。"),
                        new KnowledgeAnswerStreamClient.Completed(
                                true, "缺少退款审核状态")),
                acceptingTicketClient(), tokenClient, tokenClient,
                (identity, now) -> true,
                new ConversationWindowPolicy(8, 800, 500), ids::removeFirst,
                CLOCK, Duration.ofMinutes(30)
        );

        StepVerifier.create(service.stream(customer(),
                        new AnswerCustomerQuestion(null, "我的退款通过了吗？")))
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.SessionStarted)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Metadata)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Delta)
                .expectNext(new CustomerStreamEvent.Completed(
                        true, "缺少退款审核状态"))
                .verifyComplete();

        KnowledgeAnswerView savedAnswer = store.findById("conversation-1").block()
                .requireAttempt("attempt-1").answer();
        assertThat(savedAnswer.refused()).isTrue();
        assertThat(savedAnswer.refusalReason()).isEqualTo("缺少退款审核状态");
    }

    @Test
    void converts_a_downstream_transport_failure_to_a_terminal_error_event() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("conversation-1", "attempt-1"));
        DelegatedTokenClient tokenClient = source -> Mono.just(
                new DelegatedAccessToken("delegated-token", NOW.plusSeconds(300)));
        InMemoryConsultationSessionStore store = new InMemoryConsultationSessionStore();
        CustomerConsultationService service = new CustomerConsultationService(
                store,
                (token, request) -> Mono.just(answer()),
                (token, request) -> Flux.concat(
                        Flux.just(new KnowledgeAnswerStreamClient.Delta("退款通常在")),
                        Flux.error(new IllegalStateException("upstream connection reset"))),
                acceptingTicketClient(), tokenClient, tokenClient,
                (identity, now) -> true,
                new ConversationWindowPolicy(8, 800, 500), ids::removeFirst,
                CLOCK, Duration.ofMinutes(30)
        );

        StepVerifier.create(service.stream(customer(),
                        new AnswerCustomerQuestion(null, "退款多久到账？")))
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.SessionStarted)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Delta)
                .expectNext(new CustomerStreamEvent.Error(
                        "KNOWLEDGE_STREAM_FAILED", "回答生成中断，请稍后重试"))
                .verifyComplete();

        assertThat(store.findById("conversation-1").block()
                .requireAttempt("attempt-1").status().name()).isEqualTo("FAILED");
    }

    @Test
    void rejectsACompletedStreamWithoutTraceMetadata() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("conversation-1", "attempt-1"));
        DelegatedTokenClient tokenClient = source -> Mono.just(
                new DelegatedAccessToken("delegated-token", NOW.plusSeconds(300)));
        InMemoryConsultationSessionStore store = new InMemoryConsultationSessionStore();
        CustomerConsultationService service = new CustomerConsultationService(
                store,
                (token, request) -> Mono.just(answer()),
                (token, request) -> Flux.just(
                        new KnowledgeAnswerStreamClient.Delta("无法确认。"),
                        new KnowledgeAnswerStreamClient.Completed(
                                true, "缺少退款审核状态")),
                acceptingTicketClient(), tokenClient, tokenClient,
                (identity, now) -> true,
                new ConversationWindowPolicy(8, 800, 500), ids::removeFirst,
                CLOCK, Duration.ofMinutes(30)
        );

        StepVerifier.create(service.stream(customer(),
                        new AnswerCustomerQuestion(null, "我的退款通过了吗？")))
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.SessionStarted)
                .expectNextMatches(event -> event instanceof CustomerStreamEvent.Delta)
                .expectNext(new CustomerStreamEvent.Error(
                        "KNOWLEDGE_STREAM_FAILED", "回答生成中断，请稍后重试"))
                .verifyComplete();

        assertThat(store.findById("conversation-1").block()
                .requireAttempt("attempt-1").failureCode())
                .isEqualTo("MISSING_STREAM_METADATA");
    }

    private static CustomerConsultationService service(
            KnowledgeAnswerClient knowledge,
            TicketTaskClient ticket
    ) {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of(
                "conversation-1", "attempt-1", "attempt-2", "attempt-3"));
        IdGenerator idGenerator = ids::removeFirst;
        DelegatedTokenClient tokenClient = source -> Mono.just(
                new DelegatedAccessToken("delegated-token", NOW.plusSeconds(300)));
        ConsultationRateLimiter rateLimiter = (identity, now) -> true;
        return new CustomerConsultationService(
                new InMemoryConsultationSessionStore(), knowledge, ticket,
                tokenClient, tokenClient, rateLimiter,
                new ConversationWindowPolicy(8, 800, 500), idGenerator,
                CLOCK, Duration.ofMinutes(30)
        );
    }

    private static TicketTaskClient acceptingTicketClient() {
        return (token, key, snapshot) -> Mono.just(
                new TicketHandoffReceipt("task-100", "ACCEPTED", false));
    }

    private static CustomerAccessToken customer() {
        return new CustomerAccessToken("customer-token",
                new CustomerIdentity("customer-42", "tenant-a", List.of("customer"), List.of()));
    }

    private static KnowledgeAnswerView answer() {
        return new KnowledgeAnswerView(
                "退款通常会在 1 到 5 个工作日到账。",
                List.of(new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                false, null, "trace-123"
        );
    }
}
