package com.xiaoding.javaai.knowledge.answer.application;

import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerModel;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerTelemetry;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.answer.application.port.TraceIdProvider;
import reactor.core.publisher.Mono;

import java.util.List;

public final class KnowledgeAnswerService implements AnswerKnowledgeQuestion {

    private final PolicyContextSource contextSource;
    private final KnowledgeAnswerModel answerModel;
    private final TraceIdProvider traceIdProvider;
    private final KnowledgeAnswerTelemetry telemetry;
    private final String promptVersion;
    private final String systemInstruction;
    private final KnowledgeAnswerValidator validator = new KnowledgeAnswerValidator();

    public KnowledgeAnswerService(
            PolicyContextSource contextSource,
            KnowledgeAnswerModel answerModel,
            TraceIdProvider traceIdProvider,
            KnowledgeAnswerTelemetry telemetry,
            String promptVersion,
            String systemInstruction
    ) {
        this.contextSource = contextSource;
        this.answerModel = answerModel;
        this.traceIdProvider = traceIdProvider;
        this.telemetry = telemetry;
        this.promptVersion = promptVersion;
        this.systemInstruction = systemInstruction;
    }

    @Override
    public Mono<KnowledgeAnswer> answer(AnswerKnowledgeQuestionCommand command) {
        PolicyContextQuery query = new PolicyContextQuery(
                command.question(), command.accessScope(), command.effectiveAt()
        );
        return telemetry.observe(KnowledgeOperation.CONTEXT_LOAD, () -> contextSource.load(query))
                .flatMap(contexts -> telemetry.observe(KnowledgeOperation.MODEL_CALL, () -> answerModel.answer(new GroundedPrompt(
                        systemInstruction,
                        promptVersion,
                        command.question(),
                        command.conversationContext(),
                        contexts
                ))).map(draft -> toAnswer(validator.validate(draft, contexts), contexts)));
    }

    private KnowledgeAnswer toAnswer(ModelAnswerDraft draft, List<PolicyContext> contexts) {
        List<Citation> citations = contexts.stream()
                .filter(context -> draft.citedSectionIds().contains(context.sectionId()))
                .map(context -> new Citation(
                        context.documentId(),
                        context.version(),
                        context.sectionId(),
                        context.title()
                ))
                .toList();
        return new KnowledgeAnswer(
                draft.answer(),
                citations,
                draft.refused(),
                draft.refusalReason(),
                draft.model(),
                draft.usage(),
                draft.finishReason(),
                traceIdProvider.currentTraceId()
        );
    }
}
