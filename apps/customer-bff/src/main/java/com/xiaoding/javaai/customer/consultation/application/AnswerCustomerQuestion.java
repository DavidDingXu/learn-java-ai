package com.xiaoding.javaai.customer.consultation.application;

public record AnswerCustomerQuestion(String conversationId, String question) {
    public AnswerCustomerQuestion {
        conversationId = normalize(conversationId);
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        question = question.trim();
        if (question.length() > 2000) {
            throw new IllegalArgumentException("question exceeds 2000 characters");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
