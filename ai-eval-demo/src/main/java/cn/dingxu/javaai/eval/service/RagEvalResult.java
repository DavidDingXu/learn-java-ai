package cn.dingxu.javaai.eval.service;

public record RagEvalResult(
        String caseId,
        boolean retrievalPassed,
        boolean citationPassed,
        boolean noEvidencePassed,
        boolean passed,
        String reason
) {
    public RagEvalResult {
        reason = reason == null ? "" : reason;
    }
}
