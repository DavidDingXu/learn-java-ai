package cn.dingxu.javaai.eval.service;

import java.math.BigDecimal;

public record HarnessExperimentCase(
        String caseId,
        String scenario,
        BigDecimal minQualityImprovement,
        BigDecimal maxCostIncreaseRate,
        BigDecimal maxLatencyIncreaseRate
) {
    public HarnessExperimentCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        minQualityImprovement = nonNegative(minQualityImprovement, "minQualityImprovement");
        maxCostIncreaseRate = nonNegative(maxCostIncreaseRate, "maxCostIncreaseRate");
        maxLatencyIncreaseRate = nonNegative(maxLatencyIncreaseRate, "maxLatencyIncreaseRate");
    }

    private static BigDecimal nonNegative(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative");
        }
        return value.stripTrailingZeros();
    }
}
