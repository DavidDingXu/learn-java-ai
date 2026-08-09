package com.xiaoding.javaai.eval.model;

public record EvalCaseResult(
        String caseId,
        boolean passed,
        String reason,
        long latencyMillis,
        String traceId
) {
}
