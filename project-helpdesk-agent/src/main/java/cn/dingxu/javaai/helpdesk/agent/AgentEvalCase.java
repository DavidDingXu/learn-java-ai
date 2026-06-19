package cn.dingxu.javaai.helpdesk.agent;

public record AgentEvalCase(
        String caseId,
        AgentAdviceRequest request,
        String expectedCitationDocumentId,
        RequiredAction expectedAction
) {
    public AgentEvalCase {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("caseId must not be blank");
        }
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (expectedCitationDocumentId == null || expectedCitationDocumentId.isBlank()) {
            throw new IllegalArgumentException("expectedCitationDocumentId must not be blank");
        }
        expectedAction = expectedAction == null ? RequiredAction.NONE : expectedAction;
    }
}
