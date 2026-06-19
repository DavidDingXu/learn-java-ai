package cn.dingxu.javaai.enterprise.rag;

public record EvalCase(
        String caseId,
        String question,
        OperatorScope scope,
        String expectedDocumentId
) {
    public EvalCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        if (expectedDocumentId == null || expectedDocumentId.isBlank()) {
            throw new IllegalArgumentException("expectedDocumentId must not be blank");
        }
    }
}
