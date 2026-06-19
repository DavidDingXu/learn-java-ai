package com.xiaoding.javaai.observability.service;

import java.math.BigDecimal;

public record CostSummary(
        String scenario,
        int inputTokens,
        int outputTokens,
        BigDecimal totalCost,
        int callCount
) {
    public CostSummary {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        if (inputTokens < 0 || outputTokens < 0 || callCount < 0) {
            throw new IllegalArgumentException("summary values must not be negative");
        }
        totalCost = totalCost == null ? BigDecimal.ZERO : totalCost;
    }

    public static CostSummary empty(String scenario) {
        return new CostSummary(scenario, 0, 0, BigDecimal.ZERO, 0);
    }

    public CostSummary add(ModelUsage usage) {
        return new CostSummary(
                scenario,
                inputTokens + usage.inputTokens(),
                outputTokens + usage.outputTokens(),
                totalCost.add(usage.cost()),
                callCount + 1
        );
    }
}
