package com.xiaoding.javaai.ticket.agent.domain;

import java.time.Instant;
import java.util.Map;

public record ConfirmationRequest(
        String confirmationId,
        String actionId,
        String toolName,
        ToolRisk risk,
        String requiredRole,
        Map<String, String> arguments,
        String actionFingerprint,
        long boundTaskVersion,
        Instant expiresAt
) {
    public ConfirmationRequest {
        confirmationId = requireText(confirmationId, "confirmationId");
        actionId = requireText(actionId, "actionId");
        toolName = requireText(toolName, "toolName");
        risk = java.util.Objects.requireNonNull(risk, "risk must not be null");
        requiredRole = requireText(requiredRole, "requiredRole");
        arguments = Map.copyOf(arguments);
        actionFingerprint = requireText(actionFingerprint, "actionFingerprint");
        if (boundTaskVersion < 1) throw new IllegalArgumentException("boundTaskVersion must be positive");
        expiresAt = java.util.Objects.requireNonNull(expiresAt, "expiresAt must not be null");
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }
}
