package com.xiaoding.javaai.customer.consultation.domain;

import java.util.List;

public record ConversationContextView(String summary, List<ConversationTurn> turns) {
    public ConversationContextView {
        summary = summary == null ? "" : summary;
        turns = List.copyOf(turns);
    }
}
