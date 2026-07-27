package com.xiaoding.javaai.eval.model;

import java.time.Instant;
import java.util.List;

public record EvalReport(
        String datasetVersion,
        EvalMode mode,
        String commit,
        String model,
        String promptVersion,
        String environmentId,
        Instant executedAt,
        int passed,
        int failed,
        int skipped,
        int totalTokens,
        List<EvalCaseResult> results
) {

    public EvalReport {
        promptVersion = requireIdentifier(promptVersion, "promptVersion");
        environmentId = requireIdentifier(environmentId, "environmentId");
        results = List.copyOf(results);
    }

    private static String requireIdentifier(String value, String name) {
        if (value == null || !value.matches("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")) {
            throw new IllegalArgumentException(name + " must be a low-sensitive identifier");
        }
        return value;
    }
}
