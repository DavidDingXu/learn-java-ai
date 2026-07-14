package com.xiaoding.javaai.knowledge.answer.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

public record KnowledgeAnswerRequest(
        @NotBlank @Size(max = 2000) String question,
        @Valid ConversationContextRequest conversationContext
) {
    public KnowledgeAnswerRequest(String question) {
        this(question, ConversationContextRequest.empty());
    }

    public KnowledgeAnswerRequest {
        conversationContext = conversationContext == null
                ? ConversationContextRequest.empty() : conversationContext;
    }
}
