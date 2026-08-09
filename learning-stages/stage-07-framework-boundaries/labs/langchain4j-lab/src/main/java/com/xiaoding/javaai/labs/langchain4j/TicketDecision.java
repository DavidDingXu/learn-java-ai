package com.xiaoding.javaai.labs.langchain4j;

public record TicketDecision(String ticketId, TicketDecisionType decision, String reason) {
    public TicketDecision {
        if (ticketId == null || ticketId.isBlank() || decision == null || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("ticket decision fields must not be blank");
        }
    }
}
