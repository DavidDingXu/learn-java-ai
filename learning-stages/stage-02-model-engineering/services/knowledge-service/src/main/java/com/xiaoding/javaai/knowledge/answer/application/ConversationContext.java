package com.xiaoding.javaai.knowledge.answer.application;

import java.util.List;

public record ConversationContext(String summary, List<ConversationTurn> turns) {
    public ConversationContext {
        summary = summary == null ? "" : summary.trim();
        turns = turns == null ? List.of() : List.copyOf(turns);
    }

    public static ConversationContext empty() {
        return new ConversationContext("", List.of());
    }

    public boolean isEmpty() {
        return summary.isBlank() && turns.isEmpty();
    }
}
