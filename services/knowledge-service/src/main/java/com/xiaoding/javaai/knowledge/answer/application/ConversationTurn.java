package com.xiaoding.javaai.knowledge.answer.application;

public record ConversationTurn(ConversationRole role, String content) {
    public ConversationTurn {
        if (role == null) throw new IllegalArgumentException("role must not be null");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        content = content.trim();
    }
}
