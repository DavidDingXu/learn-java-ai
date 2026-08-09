package com.xiaoding.javaai.eval.retrieval;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RetrievalEvaluator {

    private final RetrievalEvaluationClient client;
    private final Clock clock;

    public RetrievalEvaluator(RetrievalEvaluationClient client, Clock clock) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public RetrievalEvaluationReport evaluate(
            RetrievalEvalDataset dataset,
            URI baseUrl,
            String bearerToken,
            int topK,
            String commit,
            RetrievalThresholds thresholds
    ) {
        Objects.requireNonNull(dataset, "dataset must not be null");
        Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        Objects.requireNonNull(thresholds, "thresholds must not be null");
        if (!baseUrl.isAbsolute() || !("http".equals(baseUrl.getScheme()) || "https".equals(baseUrl.getScheme()))) {
            throw new IllegalArgumentException("baseUrl must be an absolute HTTP(S) URI");
        }
        String normalizedBearerToken = normalizeOptional(bearerToken);
        String normalizedCommit = requireText(commit, "commit");
        if (topK < 1 || topK > 100) {
            throw new IllegalArgumentException("topK must be between 1 and 100");
        }
        List<RetrievalEvalResult> metricInputs = new ArrayList<>();
        List<RetrievalCaseReport> caseReports = new ArrayList<>();
        Set<String> embeddingModels = new LinkedHashSet<>();
        List<Long> latencies = new ArrayList<>();
        int executionErrors = 0;

        for (RetrievalEvalCase evalCase : dataset.cases()) {
            long startedAt = System.nanoTime();
            try {
                RetrievalClientResult result = client.retrieve(
                        baseUrl, normalizedBearerToken, evalCase.question(), topK
                );
                embeddingModels.add(result.embeddingModel());
                latencies.add(result.latencyMillis());
                metricInputs.add(new RetrievalEvalResult(
                        evalCase.id(), evalCase.expectedChunkIds(), result.chunkIds()
                ));
                caseReports.add(new RetrievalCaseReport(
                        evalCase.id(),
                        evalCase.expectedChunkIds().stream().sorted().toList(),
                        result.chunkIds(),
                        result.latencyMillis(),
                        result.embeddingModel()
                ));
            } catch (RetrievalEvaluationClientException exception) {
                long latencyMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
                executionErrors += 1;
                latencies.add(latencyMillis);
                metricInputs.add(new RetrievalEvalResult(
                        evalCase.id(), evalCase.expectedChunkIds(), List.of()
                ));
                caseReports.add(new RetrievalCaseReport(
                        evalCase.id(),
                        evalCase.expectedChunkIds().stream().sorted().toList(),
                        List.of(),
                        latencyMillis,
                        null,
                        RetrievalCaseStatus.ERROR,
                        clientErrorMessage(exception)
                ));
            }
        }

        RetrievalMetrics metrics = new RetrievalMetricsCalculator().calculate(metricInputs, topK);
        long p95LatencyMillis = percentile95(latencies);
        return new RetrievalEvaluationReport(
                dataset.version(),
                normalizedCommit,
                Instant.now(clock),
                embeddingModels,
                metrics,
                thresholds,
                p95LatencyMillis,
                executionErrors == 0
                        && embeddingModels.size() == 1
                        && thresholds.accepts(metrics, p95LatencyMillis),
                caseReports
        );
    }

    private static long percentile95(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        int index = Math.max(0, (int) Math.ceil(sorted.size() * 0.95d) - 1);
        return sorted.get(index);
    }

    private static String clientErrorMessage(RetrievalEvaluationClientException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "knowledge retrieval request failed"
                : exception.getMessage();
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        return value.strip();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
