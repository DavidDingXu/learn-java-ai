package com.xiaoding.javaai.eval.model;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ModelInteractionEvaluator {

    private final KnowledgeAnswerClient client;

    public ModelInteractionEvaluator(KnowledgeAnswerClient client) {
        this.client = client;
    }

    public EvalReport evaluate(EvalDataset dataset, URI baseUrl, EvalMode mode, String commit) {
        List<EvalCaseResult> results = new ArrayList<>();
        Set<String> models = new LinkedHashSet<>();
        int totalTokens = 0;
        for (EvalCase evalCase : dataset.cases()) {
            Instant started = Instant.now();
            try {
                KnowledgeAnswerSnapshot answer = client.answer(baseUrl, evalCase.question());
                models.add(answer.model());
                totalTokens += Math.max(answer.totalTokens(), 0);
                String failure = validate(evalCase, answer, mode);
                results.add(new EvalCaseResult(
                        evalCase.id(), failure == null, failure == null ? "ok" : failure,
                        Duration.between(started, Instant.now()).toMillis(), answer.traceId()
                ));
            } catch (RuntimeException exception) {
                results.add(new EvalCaseResult(
                        evalCase.id(), false, exception.getMessage(),
                        Duration.between(started, Instant.now()).toMillis(), "unavailable"
                ));
            }
        }
        int passed = (int) results.stream().filter(EvalCaseResult::passed).count();
        return new EvalReport(
                dataset.version(), mode, commit, modelName(models), Instant.now(),
                passed, results.size() - passed, 0, totalTokens, results
        );
    }

    private static String validate(EvalCase evalCase, KnowledgeAnswerSnapshot answer, EvalMode mode) {
        String expectedMode = mode == EvalMode.LIVE_MODEL ? "LIVE_MODEL" : "PROVIDER_PROTOCOL_FIXTURE";
        if (!expectedMode.equals(answer.executionMode())) {
            return "execution mode mismatch: expected " + expectedMode + " but was " + answer.executionMode();
        }
        if (answer.refused() && !evalCase.expectRefusal() && !evalCase.allowSafeRefusal()) {
            return "refusal expectation mismatch";
        }
        if (!answer.refused() && evalCase.expectRefusal()) {
            return "refusal expectation mismatch";
        }
        if (!answer.refused()
                && !answer.citationSectionIds().containsAll(evalCase.expectedCitationSectionIds())) {
            return "citation expectation mismatch";
        }
        for (String forbiddenPhrase : evalCase.forbiddenPhrases()) {
            if (answer.answer().contains(forbiddenPhrase)) {
                return "answer contains forbidden phrase: " + forbiddenPhrase;
            }
        }
        if (answer.totalTokens() < 0) return "token metadata is missing";
        if (answer.traceId() == null || answer.traceId().isBlank()) return "traceId is missing";
        return null;
    }

    private static String modelName(Set<String> models) {
        if (models.isEmpty()) return "unavailable";
        return models.size() == 1 ? models.iterator().next() : "mixed";
    }
}
