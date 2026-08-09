package com.xiaoding.javaai.ticket.agent.web;

import com.xiaoding.javaai.ticket.agent.application.ConfirmToolAction;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ConfirmToolActionWebRequest(
        @NotBlank @Size(max = 64) String confirmationId,
        @Positive long expectedTaskVersion,
        @NotNull ConfirmationDecision decision,
        @NotBlank @Size(max = 1000) String reason
) {
    ConfirmToolAction toApplication() {
        return new ConfirmToolAction(confirmationId, expectedTaskVersion, decision, reason);
    }
}
