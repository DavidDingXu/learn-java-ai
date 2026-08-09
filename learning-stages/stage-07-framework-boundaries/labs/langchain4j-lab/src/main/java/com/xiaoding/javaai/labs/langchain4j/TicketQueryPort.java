package com.xiaoding.javaai.labs.langchain4j;

public interface TicketQueryPort {
    TicketSnapshot find(String ticketId);
}
