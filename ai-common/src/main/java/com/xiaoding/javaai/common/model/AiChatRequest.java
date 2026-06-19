package com.xiaoding.javaai.common.model;

public record AiChatRequest(
        String userId,
        String message
) {
    public AiChatRequest {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message must not be blank");
        }
    }
}
