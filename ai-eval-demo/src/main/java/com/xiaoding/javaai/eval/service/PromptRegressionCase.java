package com.xiaoding.javaai.eval.service;

import java.math.BigDecimal;

public record PromptRegressionCase(
        String caseId,
        String promptKey,
        String baselineVersion,
        String candidateVersion,
        BigDecimal baselinePassRate,
        BigDecimal candidatePassRate,
        BigDecimal tolerance
) {
    public PromptRegressionCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (promptKey == null || promptKey.isBlank()) {
            throw new IllegalArgumentException("promptKey must not be blank");
        }
        if (baselineVersion == null || baselineVersion.isBlank()) {
            throw new IllegalArgumentException("baselineVersion must not be blank");
        }
        if (candidateVersion == null || candidateVersion.isBlank()) {
            throw new IllegalArgumentException("candidateVersion must not be blank");
        }
        baselinePassRate = normalizeRate(baselinePassRate, "baselinePassRate");
        candidatePassRate = normalizeRate(candidatePassRate, "candidatePassRate");
        tolerance = tolerance == null ? BigDecimal.ZERO : tolerance;
        if (tolerance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("tolerance must not be negative");
        }
    }

    private static BigDecimal normalizeRate(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException(fieldName + " must be between 0 and 1");
        }
        return value.stripTrailingZeros();
    }
}
