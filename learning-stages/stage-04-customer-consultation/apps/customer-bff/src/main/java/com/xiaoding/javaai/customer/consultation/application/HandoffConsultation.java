package com.xiaoding.javaai.customer.consultation.application;

public record HandoffConsultation(String conversationId, String attemptId, String reasonCode) {
    public HandoffConsultation {
        conversationId = requireText(conversationId, "conversationId");
        attemptId = requireText(attemptId, "attemptId");
        reasonCode = requireText(reasonCode, "reasonCode");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
