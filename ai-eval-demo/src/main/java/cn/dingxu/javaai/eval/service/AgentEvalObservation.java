package cn.dingxu.javaai.eval.service;

import java.util.List;

public record AgentEvalObservation(
        String caseId,
        List<String> actualToolPath,
        boolean humanApprovalRequested,
        String riskLevel
) {
    public AgentEvalObservation {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        actualToolPath = actualToolPath == null ? List.of() : List.copyOf(actualToolPath);
        riskLevel = riskLevel == null ? "" : riskLevel;
    }
}
