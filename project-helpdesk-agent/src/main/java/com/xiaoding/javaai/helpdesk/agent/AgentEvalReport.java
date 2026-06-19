package com.xiaoding.javaai.helpdesk.agent;

import java.math.BigDecimal;

public record AgentEvalReport(
        int caseCount,
        BigDecimal citationHitRate,
        BigDecimal actionAccuracy
) {
    public AgentEvalReport {
        citationHitRate = normalize(citationHitRate);
        actionAccuracy = normalize(actionAccuracy);
    }

    private static BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }
}
