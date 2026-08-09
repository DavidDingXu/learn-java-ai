package com.xiaoding.javaai.knowledge.answer.application;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeAccessScope;

import java.time.Instant;

public record AnswerKnowledgeQuestionCommand(
        String question,
        ConversationContext conversationContext,
        KnowledgeAccessScope accessScope,
        Instant effectiveAt
) {

    public AnswerKnowledgeQuestionCommand(
            String question,
            KnowledgeAccessScope accessScope,
            Instant effectiveAt
    ) {
        this(question, ConversationContext.empty(), accessScope, effectiveAt);
    }

    public AnswerKnowledgeQuestionCommand {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be blank");
        }
        if (conversationContext == null) {
            throw new IllegalArgumentException("conversationContext must not be null");
        }
        if (accessScope == null) throw new IllegalArgumentException("accessScope must not be null");
        if (effectiveAt == null) throw new IllegalArgumentException("effectiveAt must not be null");
        question = question.trim();
    }
}
