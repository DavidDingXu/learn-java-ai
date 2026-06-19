package com.xiaoding.javaai.helpdesk.agent;

import java.util.LinkedHashMap;
import java.util.Map;

class TicketRepository {

    private final Map<String, Ticket> tickets = new LinkedHashMap<>();

    void save(Ticket ticket) {
        tickets.put(ticket.ticketId(), ticket);
    }

    Ticket getVisible(String ticketId, OperatorContext operator) {
        Ticket ticket = tickets.get(ticketId);
        if (ticket == null) {
            throw new IllegalArgumentException("ticket not found: " + ticketId);
        }
        if (!ticket.accessibleBy(operator)) {
            throw new IllegalArgumentException("ticket is not visible to current operator");
        }
        return ticket;
    }

    void close(String ticketId, OperatorContext operator) {
        Ticket current = getVisible(ticketId, operator);
        tickets.put(ticketId, new Ticket(
                current.ticketId(),
                current.orderId(),
                current.tenantId(),
                current.department(),
                TicketStatus.CLOSED,
                current.customerQuestion(),
                current.refundAmount()
        ));
    }
}
