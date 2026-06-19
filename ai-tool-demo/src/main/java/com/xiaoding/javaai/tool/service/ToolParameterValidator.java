package com.xiaoding.javaai.tool.service;

import org.springframework.stereotype.Component;

@Component
public class ToolParameterValidator {

    public void requireTicketId(String ticketId) {
        requireText(ticketId, "ticketId");
        if (!ticketId.startsWith("T-")) {
            throw new IllegalArgumentException("ticketId must start with T-");
        }
    }

    public void requireOrderId(String orderId) {
        requireText(orderId, "orderId");
        if (!orderId.startsWith("O-")) {
            throw new IllegalArgumentException("orderId must start with O-");
        }
    }

    public void requireConfirmationToken(String confirmationToken) {
        requireText(confirmationToken, "confirmationToken");
        if (!confirmationToken.startsWith("confirm-")) {
            throw new IllegalArgumentException("confirmationToken must start with confirm-");
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
