package com.xiaoding.javaai.eval.service;

import java.math.BigDecimal;

public record PromptRegressionResult(
        String caseId,
        String promptKey,
        BigDecimal delta,
        boolean passed,
        String reason
) {
    public PromptRegressionResult {
        delta = delta == null ? BigDecimal.ZERO : delta.stripTrailingZeros();
        reason = reason == null ? "" : reason;
    }
}
