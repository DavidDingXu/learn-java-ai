package com.xiaoding.javaai.legacy.legacy.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class LegacyAuditRecord {

    private final String action;
    private final String ticketId;
    private final String operatorId;
    private final String tenantId;
    private final Set<String> departments;
    private final Set<String> permissions;
    private final Instant createdAt;

    public LegacyAuditRecord(String action,
                             String ticketId,
                             String operatorId,
                             String tenantId,
                             Set<String> departments,
                             Set<String> permissions,
                             Instant createdAt) {
        this.action = action;
        this.ticketId = ticketId;
        this.operatorId = operatorId;
        this.tenantId = tenantId;
        this.departments = Collections.unmodifiableSet(new HashSet<String>(departments));
        this.permissions = Collections.unmodifiableSet(new HashSet<String>(permissions));
        this.createdAt = createdAt;
    }

    public String getAction() {
        return action;
    }

    public String getTicketId() {
        return ticketId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Set<String> getDepartments() {
        return departments;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
