package com.xiaoding.javaai.observability.service;

import java.time.Instant;

public record FeedbackRecord(
        String traceId,
        String scenario,
        String rating,
        String reason,
        Instant recordedAt
) {
    public FeedbackRecord {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("scenario must not be blank");
        }
        if (rating == null || rating.isBlank()) {
            throw new IllegalArgumentException("rating must not be blank");
        }
        reason = reason == null ? "" : reason;
        recordedAt = recordedAt == null ? Instant.now() : recordedAt;
    }

    public boolean bad() {
        return "bad".equalsIgnoreCase(rating);
    }
}
