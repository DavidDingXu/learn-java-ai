package cn.dingxu.javaai.helpdesk.agent;

import java.util.LinkedHashMap;
import java.util.Map;

class OrderRepository {

    private final Map<String, Order> orders = new LinkedHashMap<>();

    void save(Order order) {
        orders.put(order.orderId(), order);
    }

    Order get(String orderId, OperatorContext operator) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("order not found: " + orderId);
        }
        if (!order.tenantId().equals(operator.tenantId())) {
            throw new IllegalArgumentException("order is not visible to current tenant");
        }
        return order;
    }
}
