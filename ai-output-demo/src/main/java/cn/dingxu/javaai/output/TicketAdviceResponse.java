package cn.dingxu.javaai.output;

import java.util.List;

public record TicketAdviceResponse(
        String summary,
        RiskLevel riskLevel,
        List<String> nextActions,
        List<String> citations
) {
    public TicketAdviceResponse {
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (riskLevel == null) {
            throw new IllegalArgumentException("riskLevel must not be null");
        }
        nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
        citations = citations == null ? List.of() : List.copyOf(citations);
        if (nextActions.isEmpty()) {
            throw new IllegalArgumentException("nextActions must not be empty");
        }
        if ((riskLevel == RiskLevel.MEDIUM || riskLevel == RiskLevel.HIGH) && citations.isEmpty()) {
            throw new IllegalArgumentException("citations must not be empty for medium or high risk advice");
        }
    }
}
