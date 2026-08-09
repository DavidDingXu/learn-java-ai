package com.xiaoding.javaai.knowledge.answer.web;

import com.xiaoding.javaai.knowledge.answer.application.ConversationRole;
import com.xiaoding.javaai.knowledge.answer.application.ConversationTurn;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConversationTurnRequest(
        @NotBlank @Pattern(regexp = "USER|ASSISTANT") String role,
        @NotBlank @Size(max = 1000) String content
) {
    ConversationTurn toApplication() {
        return new ConversationTurn(ConversationRole.valueOf(role), content);
    }
}
