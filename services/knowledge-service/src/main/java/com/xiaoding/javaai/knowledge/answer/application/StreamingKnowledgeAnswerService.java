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
    private final ExecutionMode executionMode;
    private final String promptVersion;
    private final String systemInstruction;

    public StreamingKnowledgeAnswerService(
            PolicyContextSource contextSource,
            KnowledgeAnswerStreamModel streamModel,
            TraceIdProvider traceIdProvider,
            ExecutionMode executionMode,
            String promptVersion,
            String systemInstruction
    ) {
        this.contextSource = contextSource;
        this.streamModel = streamModel;
        this.traceIdProvider = traceIdProvider;
        this.executionMode = executionMode;
        this.promptVersion = promptVersion;
        this.systemInstruction = systemInstruction;
    }

    @Override
    public Flux<AnswerStreamEvent> stream(AnswerKnowledgeQuestionCommand command) {
        return Flux.defer(() -> contextSource.load(new PolicyContextQuery(
                        command.question(), command.accessScope(), command.effectiveAt()
                ))
                .flatMapMany(contexts -> streamWithContext(command, contexts)));
    }

    private Flux<AnswerStreamEvent> streamWithContext(
            AnswerKnowledgeQuestionCommand command,
            List<PolicyContext> contexts
    ) {
        long startedNanos = System.nanoTime();
        AtomicLong firstTokenNanos = new AtomicLong();
        AtomicReference<String> model = new AtomicReference<>("unknown");
        AtomicReference<ModelUsage> usage = new AtomicReference<>(new ModelUsage(0, 0, 0));
        AtomicReference<String> finishReason = new AtomicReference<>("unknown");
        GroundedPrompt prompt = new GroundedPrompt(
                systemInstruction, promptVersion, command.question(),
                command.conversationContext(), contexts
        );

        Flux<AnswerStreamEvent> modelEvents = streamModel.stream(prompt)
                .handle((chunk, sink) -> {
                    if (chunk.model() != null && !chunk.model().isBlank()) model.set(chunk.model());
                    if (chunk.usage() != null) usage.set(chunk.usage());
                    if (chunk.finishReason() != null && !chunk.finishReason().isBlank()) {
                        finishReason.set(chunk.finishReason());
                    }
                    if (chunk.delta() != null && !chunk.delta().isEmpty()) {
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

        Flux<AnswerStreamEvent> citations = Flux.fromIterable(contexts)
                .map(context -> (AnswerStreamEvent) new AnswerStreamEvent.CitationEvent(new Citation(
                        context.documentId(), context.version(), context.sectionId(), context.title()
                )));
        Flux<AnswerStreamEvent> completed = Flux.defer(() -> {
            long first = firstTokenNanos.get();
            long ttftMillis = first == 0 ? -1 : Duration.ofNanos(first - startedNanos).toMillis();
            return Flux.just(new AnswerStreamEvent.CompletedEvent(
                    model.get(), usage.get(), finishReason.get(), ttftMillis
            ));
        });

        return Flux.concat(
                        Flux.just(new AnswerStreamEvent.MetadataEvent(
                                traceIdProvider.currentTraceId(), promptVersion, executionMode)),
                        body,
                        citations,
                        completed
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
