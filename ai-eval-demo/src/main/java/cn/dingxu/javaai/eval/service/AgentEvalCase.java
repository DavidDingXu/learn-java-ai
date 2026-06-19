package cn.dingxu.javaai.eval.service;

import java.util.List;

public record AgentEvalCase(
        String caseId,
        String question,
        List<String> expectedToolPath,
        boolean expectHumanApproval
) {
    public AgentEvalCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        expectedToolPath = expectedToolPath == null ? List.of() : List.copyOf(expectedToolPath);
    }
}
