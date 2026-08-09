package com.xiaoding.javaai.knowledge.answer.application;

import java.util.List;

public record GroundedPrompt(
        String systemInstruction,
        String promptVersion,
        String question,
        ConversationContext conversationContext,
        List<PolicyContext> contexts
) {

    public GroundedPrompt(
            String systemInstruction,
            String promptVersion,
            String question,
            List<PolicyContext> contexts
    ) {
        this(systemInstruction, promptVersion, question, ConversationContext.empty(), contexts);
    }

    public GroundedPrompt {
        if (conversationContext == null) {
            throw new IllegalArgumentException("conversationContext must not be null");
        }
        contexts = List.copyOf(contexts);
    }
}
