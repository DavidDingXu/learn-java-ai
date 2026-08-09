package com.xiaoding.javaai.customer.consultation.application;

import com.xiaoding.javaai.customer.consultation.domain.FeedbackRating;

public record RecordAnswerFeedback(
        String conversationId,
        String attemptId,
        FeedbackRating rating,
        String reasonCode,
        String comment
) {
    public RecordAnswerFeedback {
        conversationId = requireText(conversationId, "conversationId");
        attemptId = requireText(attemptId, "attemptId");
        if (rating == null) throw new IllegalArgumentException("rating must not be null");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
