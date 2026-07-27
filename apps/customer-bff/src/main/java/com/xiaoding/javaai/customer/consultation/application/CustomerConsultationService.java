package com.xiaoding.javaai.customer.consultation.application;

import com.xiaoding.javaai.customer.consultation.application.port.ConsultationRateLimiter;
import com.xiaoding.javaai.customer.consultation.application.port.ConsultationSessionStore;
import com.xiaoding.javaai.customer.consultation.application.port.IdGenerator;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerStreamClient;
import com.xiaoding.javaai.customer.consultation.application.port.TicketTaskClient;
import com.xiaoding.javaai.customer.consultation.domain.AnswerAttempt;
import com.xiaoding.javaai.customer.consultation.domain.ConsultationSession;
import com.xiaoding.javaai.customer.consultation.domain.ConversationContextView;
import com.xiaoding.javaai.customer.consultation.domain.ConversationWindowPolicy;
import com.xiaoding.javaai.customer.consultation.domain.KnowledgeAnswerView;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffReceipt;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffSnapshot;
import com.xiaoding.javaai.customer.identity.CustomerAccessToken;
import com.xiaoding.javaai.customer.identity.DelegatedTokenClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CustomerConsultationService {

    private final ConsultationSessionStore store;
    private final KnowledgeAnswerClient knowledgeClient;
    private final KnowledgeAnswerStreamClient streamClient;
    private final TicketTaskClient ticketClient;
    private final DelegatedTokenClient knowledgeTokenClient;
    private final DelegatedTokenClient ticketTokenClient;
    private final ConsultationRateLimiter rateLimiter;
    private final ConversationWindowPolicy windowPolicy;
    private final IdGenerator idGenerator;
    private final Clock clock;
    private final Duration sessionTtl;

    public CustomerConsultationService(
            ConsultationSessionStore store,
            KnowledgeAnswerClient knowledgeClient,
            TicketTaskClient ticketClient,
            DelegatedTokenClient knowledgeTokenClient,
            DelegatedTokenClient ticketTokenClient,
            ConsultationRateLimiter rateLimiter,
            ConversationWindowPolicy windowPolicy,
            IdGenerator idGenerator,
            Clock clock,
            Duration sessionTtl
    ) {
        this(store, knowledgeClient,
                (token, request) -> Flux.error(new IllegalStateException("knowledge stream is disabled")),
                ticketClient, knowledgeTokenClient, ticketTokenClient, rateLimiter,
                windowPolicy, idGenerator, clock, sessionTtl);
    }

    public CustomerConsultationService(
            ConsultationSessionStore store,
            KnowledgeAnswerClient knowledgeClient,
            KnowledgeAnswerStreamClient streamClient,
            TicketTaskClient ticketClient,
            DelegatedTokenClient knowledgeTokenClient,
            DelegatedTokenClient ticketTokenClient,
            ConsultationRateLimiter rateLimiter,
            ConversationWindowPolicy windowPolicy,
            IdGenerator idGenerator,
            Clock clock,
            Duration sessionTtl
    ) {
        this.store = store;
        this.knowledgeClient = knowledgeClient;
        this.streamClient = streamClient;
        this.ticketClient = ticketClient;
        this.knowledgeTokenClient = knowledgeTokenClient;
        this.ticketTokenClient = ticketTokenClient;
        this.rateLimiter = rateLimiter;
        this.windowPolicy = windowPolicy;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    public Mono<CustomerAnswer> answer(CustomerAccessToken customer, AnswerCustomerQuestion command) {
        return answer(customer, command, null);
    }

    public Flux<CustomerStreamEvent> stream(
            CustomerAccessToken customer,
            AnswerCustomerQuestion command
    ) {
        Instant now = Instant.now(clock);
        if (!rateLimiter.tryAcquire(customer.identity(), now)) {
            return Flux.error(new ConsultationRateLimitExceededException());
        }
        Mono<StreamState> resource = resolveSession(command.conversationId(), customer, now)
                .flatMap(session -> prepareStream(session, command.question(), now));
        return Flux.usingWhen(
                resource,
                state -> streamEvents(customer, state),
                state -> closeIncomplete(state, "STREAM_ENDED_WITHOUT_COMPLETION"),
                (state, error) -> closeIncomplete(state, "KNOWLEDGE_STREAM_FAILED"),
                state -> closeIncomplete(state, "CLIENT_CANCELLED")
        );
    }

    public Mono<CustomerAnswer> retry(CustomerAccessToken customer, RetryCustomerAnswer command) {
        Instant now = Instant.now(clock);
        return loadOwned(command.conversationId(), customer, now)
                .flatMap(session -> {
                    AnswerAttempt original = session.requireAttempt(command.attemptId());
                    return answer(customer,
                            new AnswerCustomerQuestion(command.conversationId(), original.question()),
                            original.attemptId());
                });
    }

    public Mono<Void> recordFeedback(CustomerAccessToken customer, RecordAnswerFeedback command) {
        Instant now = Instant.now(clock);
        return loadOwned(command.conversationId(), customer, now)
                .flatMap(session -> store.save(session.recordFeedback(
                        command.attemptId(), command.rating(), command.reasonCode(), command.comment(),
                        now, sessionTtl), session.version()))
                .then();
    }

    public Mono<TicketHandoffReceipt> handoff(
            CustomerAccessToken customer,
            HandoffConsultation command
    ) {
        Instant now = Instant.now(clock);
        return loadOwned(command.conversationId(), customer, now)
                .map(session -> session.createHandoffSnapshot(
                        command.attemptId(), command.reasonCode(), now))
                .flatMap(snapshot -> createTicket(customer, snapshot));
    }

    private Mono<CustomerAnswer> answer(
            CustomerAccessToken customer,
            AnswerCustomerQuestion command,
            String retryOfAttemptId
    ) {
        Instant now = Instant.now(clock);
        if (!rateLimiter.tryAcquire(customer.identity(), now)) {
            return Mono.error(new ConsultationRateLimitExceededException());
        }
        return resolveSession(command.conversationId(), customer, now)
                .flatMap(session -> startAndCallKnowledge(
                        session, customer, command.question(), retryOfAttemptId, now));
    }

    private Mono<CustomerAnswer> startAndCallKnowledge(
            ConsultationSession current,
            CustomerAccessToken customer,
            String question,
            String retryOfAttemptId,
            Instant now
    ) {
        ConsultationSession compacted = windowPolicy.compact(current, now, sessionTtl);
        ConversationContextView context = compacted.context();
        String attemptId = idGenerator.nextId();
        ConsultationSession started = compacted.startAttempt(
                attemptId, question, retryOfAttemptId, now, sessionTtl);
        KnowledgeAnswerClient.Request request = new KnowledgeAnswerClient.Request(question, context);

        return store.save(started, current.version())
                .flatMap(saved -> knowledgeTokenClient.exchange(customer)
                        .flatMap(token -> knowledgeClient.answer(token, request))
                        .flatMap(answer -> complete(saved.conversationId(), attemptId, answer, Instant.now(clock))
                                .map(completed -> toCustomerAnswer(completed, attemptId)))
                        .onErrorResume(error -> markFailed(
                                        saved.conversationId(), attemptId, Instant.now(clock))
                                .then(Mono.error(error))));
    }

    private Mono<StreamState> prepareStream(
            ConsultationSession current,
            String question,
            Instant now
    ) {
        ConsultationSession compacted = windowPolicy.compact(current, now, sessionTtl);
        ConversationContextView context = compacted.context();
        String attemptId = idGenerator.nextId();
        ConsultationSession started = compacted.startAttempt(
                attemptId, question, null, now, sessionTtl);
        return store.save(started, current.version())
                .map(saved -> new StreamState(
                        saved.conversationId(), attemptId,
                        new KnowledgeAnswerClient.Request(question, context)));
    }

    private Flux<CustomerStreamEvent> streamEvents(
            CustomerAccessToken customer,
            StreamState state
    ) {
        Flux<CustomerStreamEvent> downstream = knowledgeTokenClient.exchange(customer)
                .flatMapMany(token -> streamClient.stream(token, state.request))
                .concatMap(event -> mapStreamEvent(state, event))
                .onErrorResume(error -> failStream(state, "KNOWLEDGE_STREAM_FAILED")
                        .thenReturn(new CustomerStreamEvent.Error(
                                "KNOWLEDGE_STREAM_FAILED", "回答生成中断，请稍后重试")));
        return downstream.startWith(new CustomerStreamEvent.SessionStarted(
                state.conversationId, state.attemptId, null));
    }

    private Mono<CustomerStreamEvent> mapStreamEvent(
            StreamState state,
            KnowledgeAnswerStreamClient.Event event
    ) {
        return switch (event) {
            case KnowledgeAnswerStreamClient.Metadata metadata -> {
                state.traceId = metadata.traceId();
                yield Mono.just(new CustomerStreamEvent.Metadata(metadata.traceId()));
            }
            case KnowledgeAnswerStreamClient.Delta delta -> {
                state.answer.append(delta.text());
                yield Mono.just(new CustomerStreamEvent.Delta(delta.text()));
            }
            case KnowledgeAnswerStreamClient.Heartbeat heartbeat -> Mono.just(
                    new CustomerStreamEvent.Heartbeat(heartbeat.epochMillis()));
            case KnowledgeAnswerStreamClient.Citation citation -> {
                state.citations.add(citation.citation());
                yield Mono.just(new CustomerStreamEvent.Citation(citation.citation()));
            }
            case KnowledgeAnswerStreamClient.Completed completed -> completeStream(state, completed)
                    .thenReturn(new CustomerStreamEvent.Completed(
                            completed.refused(), completed.refusalReason()));
            case KnowledgeAnswerStreamClient.Error error -> failStream(state, error.code())
                    .thenReturn(new CustomerStreamEvent.Error(error.code(), error.message()));
        };
    }

    private Mono<Void> completeStream(
            StreamState state,
            KnowledgeAnswerStreamClient.Completed completed
    ) {
        if (state.answer.isEmpty()) {
            return failStream(state, "EMPTY_STREAM_ANSWER")
                    .then(Mono.error(new IllegalStateException("knowledge stream completed without an answer")));
        }
        if (state.traceId == null || state.traceId.isBlank()) {
            return failStream(state, "MISSING_STREAM_METADATA")
                    .then(Mono.error(new IllegalStateException(
                            "knowledge stream completed without metadata"
                    )));
        }
        KnowledgeAnswerView answer = new KnowledgeAnswerView(
                state.answer.toString(), List.copyOf(state.citations),
                completed.refused(), completed.refusalReason(), state.traceId);
        return complete(state.conversationId, state.attemptId, answer, Instant.now(clock))
                .doOnSuccess(ignored -> state.terminal.set(true))
                .then();
    }

    private Mono<Void> failStream(StreamState state, String code) {
        if (!state.terminal.compareAndSet(false, true)) return Mono.empty();
        return markFailed(state.conversationId, state.attemptId, code, Instant.now(clock));
    }

    private Mono<Void> closeIncomplete(StreamState state, String code) {
        return state.terminal.get() ? Mono.empty() : failStream(state, code);
    }

    private Mono<ConsultationSession> complete(
            String conversationId,
            String attemptId,
            KnowledgeAnswerView answer,
            Instant now
    ) {
        return store.findById(conversationId)
                .flatMap(current -> store.save(
                        current.completeAttempt(attemptId, answer, now, sessionTtl),
                        current.version()));
    }

    private Mono<Void> markFailed(String conversationId, String attemptId, Instant now) {
        return markFailed(conversationId, attemptId, "KNOWLEDGE_CALL_FAILED", now);
    }

    private Mono<Void> markFailed(
            String conversationId,
            String attemptId,
            String code,
            Instant now
    ) {
        return store.findById(conversationId)
                .flatMap(current -> store.save(
                        current.failAttempt(attemptId, code, now, sessionTtl),
                        current.version()))
                .onErrorResume(ignored -> Mono.empty())
                .then();
    }

    private Mono<TicketHandoffReceipt> createTicket(
            CustomerAccessToken customer,
            TicketHandoffSnapshot snapshot
    ) {
        return ticketTokenClient.exchange(customer)
                .flatMap(token -> ticketClient.createHandoff(
                        token, snapshot.idempotencyKey(), snapshot));
    }

    private Mono<ConsultationSession> resolveSession(
            String conversationId,
            CustomerAccessToken customer,
            Instant now
    ) {
        if (conversationId == null) {
            return store.create(ConsultationSession.start(
                    idGenerator.nextId(),
                    customer.identity().tenantId(),
                    customer.identity().subject(),
                    now,
                    sessionTtl
            ));
        }
        return loadOwned(conversationId, customer, now);
    }

    private Mono<ConsultationSession> loadOwned(
            String conversationId,
            CustomerAccessToken customer,
            Instant now
    ) {
        return store.findById(conversationId)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("unknown conversation")))
                .map(session -> {
                    try {
                        session.requireOwner(customer.identity().tenantId(),
                                customer.identity().subject(), now);
                        return session;
                    } catch (SecurityException error) {
                        throw new ConversationAccessDeniedException();
                    }
                });
    }

    private static CustomerAnswer toCustomerAnswer(ConsultationSession session, String attemptId) {
        AnswerAttempt attempt = session.requireAttempt(attemptId);
        KnowledgeAnswerView answer = attempt.answer();
        return new CustomerAnswer(
                session.conversationId(),
                attemptId,
                attempt.retryOfAttemptId(),
                answer.answer(),
                answer.citations(),
                answer.refused(),
                answer.refusalReason(),
                answer.traceId()
        );
    }

    private static final class StreamState {
        private final String conversationId;
        private final String attemptId;
        private final KnowledgeAnswerClient.Request request;
        private final StringBuilder answer = new StringBuilder();
        private final List<com.xiaoding.javaai.customer.consultation.domain.CitationView> citations =
                new ArrayList<>();
        private final AtomicBoolean terminal = new AtomicBoolean();
        private String traceId;

        private StreamState(
                String conversationId,
                String attemptId,
                KnowledgeAnswerClient.Request request
        ) {
            this.conversationId = conversationId;
            this.attemptId = attemptId;
            this.request = request;
        }
    }
}
