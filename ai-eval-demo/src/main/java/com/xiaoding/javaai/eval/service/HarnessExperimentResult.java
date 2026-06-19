package com.xiaoding.javaai.eval.service;

import java.math.BigDecimal;

public record HarnessExperimentResult(
        String caseId,
        String scenario,
        String winner,
        boolean promotable,
        BigDecimal qualityDelta,
        BigDecimal costIncreaseRate,
        BigDecimal latencyIncreaseRate,
        String reason
) {
    public HarnessExperimentResult {
        qualityDelta = qualityDelta == null ? BigDecimal.ZERO : qualityDelta.stripTrailingZeros();
        costIncreaseRate = costIncreaseRate == null ? BigDecimal.ZERO : costIncreaseRate.stripTrailingZeros();
        latencyIncreaseRate = latencyIncreaseRate == null ? BigDecimal.ZERO : latencyIncreaseRate.stripTrailingZeros();
        reason = reason == null ? "" : reason;
    }
}
