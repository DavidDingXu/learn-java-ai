package com.xiaoding.javaai.legacy.legacy.model;

public class TicketRecord {

    private final String ticketId;
    private final String tenantId;
    private final String department;
    private final String status;
    private final String content;

    public TicketRecord(String ticketId, String tenantId, String department, String status, String content) {
        this.ticketId = ticketId;
        this.tenantId = tenantId;
        this.department = department;
        this.status = status;
        this.content = content;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getDepartment() {
        return department;
    }

    public String getStatus() {
        return status;
    }

    public String getContent() {
        return content;
    }
}
