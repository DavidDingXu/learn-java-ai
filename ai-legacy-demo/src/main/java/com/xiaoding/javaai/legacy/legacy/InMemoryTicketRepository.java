package com.xiaoding.javaai.legacy.legacy;

import com.xiaoding.javaai.legacy.legacy.model.TicketRecord;

import java.util.LinkedHashMap;
import java.util.Map;

public class InMemoryTicketRepository {

    private final Map<String, TicketRecord> tickets = new LinkedHashMap<String, TicketRecord>();

    public void save(TicketRecord ticketRecord) {
        tickets.put(ticketRecord.getTicketId(), ticketRecord);
    }

    public TicketRecord getRequired(String ticketId) {
        TicketRecord ticketRecord = tickets.get(ticketId);
        if (ticketRecord == null) {
            throw new IllegalArgumentException("ticket not found: " + ticketId);
        }
        return ticketRecord;
    }
}
