package com.xiaoding.javaai.enterprise.rag;

import java.math.BigDecimal;

public record EvalReport(
        int caseCount,
        BigDecimal retrievalHitRate,
        BigDecimal citationHitRate
) {
    public EvalReport {
        retrievalHitRate = normalize(retrievalHitRate);
        citationHitRate = normalize(citationHitRate);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }
}
