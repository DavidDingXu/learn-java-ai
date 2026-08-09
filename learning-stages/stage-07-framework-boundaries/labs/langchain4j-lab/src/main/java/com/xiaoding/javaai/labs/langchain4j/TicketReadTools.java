package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class TicketReadTools {

    private final TicketQueryPort queryPort;
    private final AtomicInteger invocationCount = new AtomicInteger();

    public TicketReadTools(TicketQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort must not be null");
    }

    @Tool(name = "query_ticket", value = "按工单编号查询工单状态和低敏摘要")
    public TicketSnapshot queryTicket(
            @P(name = "ticket_id", description = "工单编号") String ticketId) {
        invocationCount.incrementAndGet();
        return queryPort.find(ticketId);
    }

    public int invocationCount() {
        return invocationCount.get();
    }
}
