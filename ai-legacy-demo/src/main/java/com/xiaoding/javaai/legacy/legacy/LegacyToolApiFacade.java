package com.xiaoding.javaai.legacy.legacy;

import com.xiaoding.javaai.legacy.legacy.model.OperatorContext;
import com.xiaoding.javaai.legacy.legacy.model.TicketRecord;
import com.xiaoding.javaai.legacy.legacy.model.TicketSnapshot;

public class LegacyToolApiFacade {

    private final InMemoryTicketRepository ticketRepository;

    public LegacyToolApiFacade(InMemoryTicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public TicketSnapshot queryTicket(String ticketId, OperatorContext operatorContext) {
        TicketRecord ticketRecord = ticketRepository.getRequired(ticketId);
        verifyCanRead(ticketRecord, operatorContext);
        return new TicketSnapshot(ticketRecord.getTicketId(), ticketRecord.getStatus(), ticketRecord.getContent());
    }

    public void verifyCanRead(TicketRecord ticketRecord, OperatorContext operatorContext) {
        if (!operatorContext.hasPermission("TICKET_READ")) {
            throw new IllegalArgumentException("operator missing permission: TICKET_READ");
        }
        if (!ticketRecord.getTenantId().equals(operatorContext.getTenantId())) {
            throw new IllegalArgumentException("operator cannot access ticket tenant");
        }
        if (!operatorContext.canAccessDepartment(ticketRecord.getDepartment())) {
            throw new IllegalArgumentException("operator cannot access ticket department");
        }
    }
}
