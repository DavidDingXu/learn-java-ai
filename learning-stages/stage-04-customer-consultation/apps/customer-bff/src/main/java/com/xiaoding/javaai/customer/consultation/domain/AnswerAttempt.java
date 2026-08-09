package com.xiaoding.javaai.customer.consultation.domain;

import java.time.Instant;

public record AnswerAttempt(
        String attemptId,
        String question,
        String retryOfAttemptId,
        AnswerAttemptStatus status,
        KnowledgeAnswerView answer,
        String failureCode,
        Instant createdAt,
        Instant completedAt
) {
    public static AnswerAttempt pending(
            String attemptId,
            String question,
            String retryOfAttemptId,
            Instant createdAt
    ) {
        return new AnswerAttempt(
                requireText(attemptId, "attemptId"),
                requireText(question, "question"),
                optionalText(retryOfAttemptId),
                AnswerAttemptStatus.PENDING,
                null,
                null,
                createdAt,
                null
        );
    }

    public AnswerAttempt complete(KnowledgeAnswerView result, Instant completedAt) {
        requirePending();
        if (result == null) throw new IllegalArgumentException("result must not be null");
        return new AnswerAttempt(attemptId, question, retryOfAttemptId,
                AnswerAttemptStatus.COMPLETED, result, null, createdAt, completedAt);
    }

    public AnswerAttempt fail(String code, Instant completedAt) {
        requirePending();
        return new AnswerAttempt(attemptId, question, retryOfAttemptId,
                AnswerAttemptStatus.FAILED, null, requireText(code, "code"), createdAt, completedAt);
    }

    private void requirePending() {
        if (status != AnswerAttemptStatus.PENDING) {
            throw new IllegalStateException("answer attempt is not pending: " + attemptId);
        }
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
