package com.xiaoding.javaai.eval.model;

import java.time.Instant;
import java.util.List;

public record EvalReport(
        String datasetVersion,
        EvalMode mode,
        String commit,
        String model,
        Instant executedAt,
        int passed,
        int failed,
        int skipped,
        int totalTokens,
        List<EvalCaseResult> results
) {

    public EvalReport {
        results = List.copyOf(results);
    }
}
