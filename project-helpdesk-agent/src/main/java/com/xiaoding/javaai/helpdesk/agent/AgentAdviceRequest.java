package com.xiaoding.javaai.helpdesk.agent;

public record AgentAdviceRequest(String ticketId, String question, OperatorContext operator) {
    public AgentAdviceRequest {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId must not be blank");
        }
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (operator == null) {
            throw new IllegalArgumentException("operator must not be null");
        }
    }
}
