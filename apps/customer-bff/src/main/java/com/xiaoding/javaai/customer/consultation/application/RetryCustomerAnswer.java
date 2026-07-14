package com.xiaoding.javaai.customer.consultation.application;

public record RetryCustomerAnswer(String conversationId, String attemptId) {
    public RetryCustomerAnswer {
        conversationId = requireText(conversationId, "conversationId");
        attemptId = requireText(attemptId, "attemptId");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
