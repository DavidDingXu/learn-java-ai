package cn.dingxu.javaai.legacy.legacy.model;

public class TicketSnapshot {

    private static final String CONTRACT_NAME = "Legacy Tool API";

    private final String ticketId;
    private final String status;
    private final String content;

    public TicketSnapshot(String ticketId, String status, String content) {
        this.ticketId = ticketId;
        this.status = status;
        this.content = content;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }

    public String getContractName() {
        return CONTRACT_NAME;
    }
}
