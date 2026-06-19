package com.xiaoding.javaai.eval.service;

public record JudgeCalibrationCase(
        String caseId,
        String question,
        boolean humanPassed,
        boolean judgePassed,
        double judgeConfidence,
        String note
) {
    public JudgeCalibrationCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (judgeConfidence < 0 || judgeConfidence > 1) {
            throw new IllegalArgumentException("judgeConfidence must be between 0 and 1");
        }
        note = note == null ? "" : note;
    }
}
