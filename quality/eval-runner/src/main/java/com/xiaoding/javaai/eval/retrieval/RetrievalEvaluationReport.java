package com.xiaoding.javaai.eval.retrieval;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record RetrievalEvaluationReport(
        String datasetVersion,
        String commit,
        Instant executedAt,
        Set<String> embeddingModels,
        RetrievalMetrics metrics,
        RetrievalThresholds thresholds,
        long p95LatencyMillis,
        boolean passed,
        List<RetrievalCaseReport> cases
) {
    public RetrievalEvaluationReport {
        embeddingModels = Collections.unmodifiableSet(new LinkedHashSet<>(embeddingModels));
        cases = List.copyOf(cases);
    }
}
