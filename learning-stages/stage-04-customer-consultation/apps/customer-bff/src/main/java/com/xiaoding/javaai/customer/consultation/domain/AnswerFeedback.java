package com.xiaoding.javaai.customer.consultation.domain;

import java.time.Instant;

public record AnswerFeedback(
        String attemptId,
        FeedbackRating rating,
        String reasonCode,
        String comment,
        Instant createdAt
) {
    public AnswerFeedback {
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("attemptId must not be blank");
        }
        if (rating == null) throw new IllegalArgumentException("rating must not be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");
        attemptId = attemptId.trim();
        reasonCode = normalize(reasonCode, 64, "reasonCode");
        comment = normalize(comment, 500, "comment");
        if (rating == FeedbackRating.NOT_HELPFUL && reasonCode == null) {
            throw new IllegalArgumentException("reasonCode is required for NOT_HELPFUL feedback");
        }
    }

    private static String normalize(String value, int maxLength, String name) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(name + " exceeds " + maxLength + " characters");
        }
        return normalized;
    }
}
