package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.ConfirmationDecision;

public record ConfirmToolAction(
        String confirmationId,
        long expectedTaskVersion,
        ConfirmationDecision decision,
        String reason
) {
    public ConfirmToolAction {
        confirmationId = requireText(confirmationId, "confirmationId");
        if (expectedTaskVersion < 1) throw new IllegalArgumentException("expectedTaskVersion must be positive");
        decision = java.util.Objects.requireNonNull(decision, "decision must not be null");
        reason = requireText(reason, "reason");
        if (reason.length() > 1000) throw new IllegalArgumentException("reason exceeds 1000 characters");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
