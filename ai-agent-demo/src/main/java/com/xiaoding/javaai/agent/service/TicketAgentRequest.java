package com.xiaoding.javaai.agent.service;

public record TicketAgentRequest(
        String ticketId,
        String userQuestion,
        String userId,
        String tenantId,
        String department
) {
    public TicketAgentRequest {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId must not be blank");
        }
        if (userQuestion == null || userQuestion.isBlank()) {
            throw new IllegalArgumentException("userQuestion must not be blank");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("department must not be blank");
        }
    }
}
