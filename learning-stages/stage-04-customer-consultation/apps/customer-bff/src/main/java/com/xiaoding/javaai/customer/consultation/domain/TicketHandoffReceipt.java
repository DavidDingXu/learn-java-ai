package com.xiaoding.javaai.customer.consultation.domain;

public record TicketHandoffReceipt(String taskId, String status, boolean duplicate) {
    public TicketHandoffReceipt {
        if (taskId == null || taskId.isBlank()) throw new IllegalArgumentException("taskId must not be blank");
        if (status == null || status.isBlank()) throw new IllegalArgumentException("status must not be blank");
    }
}
