package com.xiaoding.javaai.legacy.ticket;

public final class ConfirmToolActionRequest {

    private final String confirmationId;
    private final long expectedTaskVersion;
    private final String decision;
    private final String reason;

    public ConfirmToolActionRequest(
            String confirmationId,
            long expectedTaskVersion,
            String decision,
            String reason
    ) {
        this.confirmationId = requireText(confirmationId, "confirmationId");
        if (expectedTaskVersion <= 0) {
            throw new IllegalArgumentException("expectedTaskVersion must be positive");
        }
        this.expectedTaskVersion = expectedTaskVersion;
        this.decision = requireDecision(decision);
        this.reason = requireText(reason, "reason");
        if (this.reason.length() > 1000) throw new IllegalArgumentException("reason exceeds 1000 characters");
    }

    public String getConfirmationId() {
        return confirmationId;
    }

    public long getExpectedTaskVersion() {
        return expectedTaskVersion;
    }

    public String getDecision() {
        return decision;
    }

    public String getReason() {
        return reason;
    }

    private static String requireDecision(String value) {
        String normalized = requireText(value, "decision");
        if (!"APPROVE".equals(normalized) && !"REJECT".equals(normalized)) {
            throw new IllegalArgumentException("decision must be APPROVE or REJECT");
        }
        return normalized;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value.trim();
    }
}
