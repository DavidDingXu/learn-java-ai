package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.ConversationContext;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ConversationContextRequest(
        @Size(max = 1000) String summary,
        @Size(max = 12) List<@Valid ConversationTurnRequest> turns
) {
    public ConversationContextRequest {
        summary = summary == null ? "" : summary;
        turns = turns == null ? List.of() : List.copyOf(turns);
    }

    static ConversationContextRequest empty() {
        return new ConversationContextRequest("", List.of());
    }

    ConversationContext toApplication() {
        return new ConversationContext(
                summary,
                turns.stream().map(ConversationTurnRequest::toApplication).toList()
        );
    }
}
