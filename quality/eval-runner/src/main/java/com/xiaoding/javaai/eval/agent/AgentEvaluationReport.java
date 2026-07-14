package com.xiaoding.javaai.eval.agent;

import java.time.Instant;
import java.util.List;

public record AgentEvaluationReport(
        String datasetVersion,
        String commit,
        Instant executedAt,
        int passedCount,
        int failedCount,
        boolean passed,
        List<AgentCaseReport> cases
) {
    public AgentEvaluationReport {
        cases = List.copyOf(cases);
    }
}
