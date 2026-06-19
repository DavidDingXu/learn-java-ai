package cn.dingxu.javaai.eval.service;

public record JudgeCalibrationResult(
        String caseId,
        boolean agreed,
        boolean lowConfidence,
        boolean passed,
        String reason
) {
    public JudgeCalibrationResult {
        reason = reason == null ? "" : reason;
    }
}
