package com.xiaoding.javaai.knowledge.answer.application;

import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerStreamModel;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.answer.application.port.TraceIdProvider;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public final class StreamingKnowledgeAnswerService implements StreamKnowledgeAnswer {

    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(10);

    private final PolicyContextSource contextSource;
    private final KnowledgeAnswerStreamModel streamModel;
    private final TraceIdProvider traceIdProvider;
    private final String promptVersion;
    private final String systemInstruction;
    private final KnowledgeAnswerValidator validator = new KnowledgeAnswerValidator();

    public StreamingKnowledgeAnswerService(
            PolicyContextSource contextSource,
            KnowledgeAnswerStreamModel streamModel,
            TraceIdProvider traceIdProvider,
            String promptVersion,
            String systemInstruction
    ) {
        this.contextSource = contextSource;
        this.streamModel = streamModel;
        this.traceIdProvider = traceIdProvider;
        this.promptVersion = promptVersion;
        this.systemInstruction = systemInstruction;
    }

    @Override
    public Flux<AnswerStreamEvent> stream(AnswerKnowledgeQuestionCommand command) {
        return Flux.defer(() -> {
            long startedNanos = System.nanoTime();
            return contextSource.load(new PolicyContextQuery(
                        command.question(), command.accessScope(), command.effectiveAt()
                ))
                    .flatMapMany(contexts -> streamWithContext(command, contexts, startedNanos));
        });
    }

    private Flux<AnswerStreamEvent> streamWithContext(
            AnswerKnowledgeQuestionCommand command,
            List<PolicyContext> contexts,
            long startedNanos
    ) {
        AtomicLong firstTokenNanos = new AtomicLong();
        AtomicReference<String> model = new AtomicReference<>();
        AtomicReference<ModelUsage> usage = new AtomicReference<>();
        AtomicReference<String> finishReason = new AtomicReference<>("unknown");
        AtomicReference<ModelStreamDecision> decision = new AtomicReference<>();
        StringBuilder answer = new StringBuilder();
        GroundedPrompt prompt = new GroundedPrompt(
                systemInstruction, promptVersion, command.question(),
                command.conversationContext(), contexts
        );

        Flux<AnswerStreamEvent> modelEvents = streamModel.stream(prompt)
                .<AnswerStreamEvent>handle((chunk, sink) -> {
                    if (chunk.model() != null && !chunk.model().isBlank()) model.set(chunk.model());
                    if (chunk.usage() != null) usage.set(chunk.usage());
                    if (chunk.finishReason() != null && !chunk.finishReason().isBlank()) {
                        finishReason.set(chunk.finishReason());
                    }
                    if (chunk.decision() != null) {
                        if (!decision.compareAndSet(null, chunk.decision())) {
                            throw new InvalidModelAnswerException("model stream decision is duplicated");
                        }
                        validator.validateDecision(chunk.decision(), contexts);
                    }
                    if (chunk.delta() != null && !chunk.delta().isEmpty()) {
                        if (decision.get() == null) {
                            throw new InvalidModelAnswerException(
                                    "model stream emitted answer text before its decision"
                            );
                        }
                        answer.append(chunk.delta());
                        firstTokenNanos.compareAndSet(0, System.nanoTime());
                        sink.next(new AnswerStreamEvent.DeltaEvent(chunk.delta()));
                    }
                });

        Flux<AnswerStreamEvent> body = modelEvents.publish(shared -> Flux.merge(
                shared,
                Flux.interval(HEARTBEAT_INTERVAL)
                        .map(ignored -> (AnswerStreamEvent) new AnswerStreamEvent.HeartbeatEvent(
                                System.currentTimeMillis()))
                        .takeUntilOther(shared.ignoreElements())
        ));

        Flux<AnswerStreamEvent> terminal = Flux.defer(() -> {
            ModelStreamDecision terminalDecision = decision.get();
            validator.validateDecision(terminalDecision, contexts);
            ModelAnswerDraft draft = validator.validate(new ModelAnswerDraft(
                    answer.toString(),
                    terminalDecision.citedSectionIds(),
                    terminalDecision.refused(),
                    terminalDecision.refusalReason(),
                    model.get(),
                    usage.get(),
                    finishReason.get()
            ), contexts);
            long first = firstTokenNanos.get();
            long ttftMillis = first == 0 ? -1 : Duration.ofNanos(first - startedNanos).toMillis();
            Flux<AnswerStreamEvent> citations = Flux.fromIterable(contexts)
                    .filter(context -> draft.citedSectionIds().contains(context.sectionId()))
                    .map(context -> (AnswerStreamEvent) new AnswerStreamEvent.CitationEvent(new Citation(
                            context.documentId(), context.version(), context.sectionId(), context.title()
                    )));
            return Flux.concat(citations, Flux.just(new AnswerStreamEvent.CompletedEvent(
                    draft.model(), draft.usage(), draft.finishReason(), ttftMillis,
                    draft.refused(), draft.refusalReason()
            )));
        });

        return Flux.concat(
                        Flux.just(new AnswerStreamEvent.MetadataEvent(
                                traceIdProvider.currentTraceId(), promptVersion)),
                        body,
                        terminal
                )
                .onErrorResume(error -> Flux.just(new AnswerStreamEvent.ErrorEvent(
                        "MODEL_STREAM_FAILED", safeMessage(error)
                )));
    }

    private static String safeMessage(Throwable error) {
        return error instanceof ModelNotConfiguredException
                ? error.getMessage()
                : "The model stream ended unexpectedly";
    }

}
