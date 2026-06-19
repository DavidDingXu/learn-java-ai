package cn.dingxu.javaai.helpdesk.agent;

public record Ticket(
        String ticketId,
        String orderId,
        String tenantId,
        String department,
        TicketStatus status,
        String customerQuestion,
        int refundAmount
) {
    public Ticket {
        if (ticketId == null || ticketId.isBlank()) {
            throw new IllegalArgumentException("ticketId must not be blank");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (department == null || department.isBlank()) {
            throw new IllegalArgumentException("department must not be blank");
        }
        status = status == null ? TicketStatus.OPEN : status;
        customerQuestion = customerQuestion == null ? "" : customerQuestion;
    }

    boolean accessibleBy(OperatorContext operator) {
        return tenantId.equals(operator.tenantId()) && department.equals(operator.department());
    }
}
