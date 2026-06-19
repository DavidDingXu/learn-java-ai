package com.xiaoding.javaai.helpdesk.agent;

public record Order(
        String orderId,
        String tenantId,
        OrderShippingStatus shippingStatus,
        int amount
) {
    public Order {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        shippingStatus = shippingStatus == null ? OrderShippingStatus.CREATED : shippingStatus;
    }
}
