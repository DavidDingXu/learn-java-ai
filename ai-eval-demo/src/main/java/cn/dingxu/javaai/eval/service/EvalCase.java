package cn.dingxu.javaai.eval.service;

public record EvalCase(String caseId, String question, String expectedKeyword, String actualAnswer) {
    public EvalCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (expectedKeyword == null || expectedKeyword.isBlank()) {
            throw new IllegalArgumentException("expectedKeyword must not be blank");
        }
        actualAnswer = actualAnswer == null ? "" : actualAnswer;
    }
}
