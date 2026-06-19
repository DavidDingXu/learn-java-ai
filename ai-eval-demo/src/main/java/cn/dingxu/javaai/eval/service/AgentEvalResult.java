package cn.dingxu.javaai.eval.service;

public record AgentEvalResult(
        String caseId,
        boolean pathPassed,
        boolean approvalPassed,
        boolean riskPassed,
        boolean passed,
        String reason
) {
    public AgentEvalResult {
        reason = reason == null ? "" : reason;
    }
}
