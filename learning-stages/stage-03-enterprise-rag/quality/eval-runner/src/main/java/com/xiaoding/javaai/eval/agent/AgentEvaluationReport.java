package com.xiaoding.javaai.eval.agent;

import java.time.Instant;
import java.util.List;

public record AgentEvaluationReport(
        String datasetVersion,
        String commit,
        String runId,
        Instant executedAt,
        int passedCount,
        int failedCount,
        boolean passed,
        List<AgentCaseReport> cases
) {
    public AgentEvaluationReport {
        if (runId == null || runId.isBlank()) throw new IllegalArgumentException("runId must not be blank");
        runId = runId.trim();
        cases = List.copyOf(cases);
    }
}
