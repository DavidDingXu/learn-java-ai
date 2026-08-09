package com.xiaoding.javaai.customer.consultation.domain;

import java.time.Instant;

public record ConversationTurn(
        ConversationRole role,
        String content,
        String attemptId,
        Instant createdAt
) {
    public ConversationTurn {
        if (role == null) throw new IllegalArgumentException("role must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("attemptId must not be blank");
        }
        if (createdAt == null) throw new IllegalArgumentException("createdAt must not be null");
        content = content.trim();
        attemptId = attemptId.trim();
    }
}
