package com.xiaoding.javaai.eval.service;

import java.math.BigDecimal;

public record HarnessStrategyObservation(
        String caseId,
        String strategy,
        BigDecimal qualityScore,
        int costUnits,
        int latencyMillis
) {
    public HarnessStrategyObservation {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("strategy must not be blank");
        }
        if (qualityScore == null || qualityScore.compareTo(BigDecimal.ZERO) < 0 || qualityScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("qualityScore must be between 0 and 1");
        }
        if (costUnits < 0) {
            throw new IllegalArgumentException("costUnits must not be negative");
        }
        if (latencyMillis < 0) {
            throw new IllegalArgumentException("latencyMillis must not be negative");
        }
        qualityScore = qualityScore.stripTrailingZeros();
    }
}
