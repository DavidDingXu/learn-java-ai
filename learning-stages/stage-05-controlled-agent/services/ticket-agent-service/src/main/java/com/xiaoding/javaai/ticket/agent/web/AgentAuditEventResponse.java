package com.xiaoding.javaai.ticket.agent.web;

import com.xiaoding.javaai.ticket.agent.application.AgentAuditEvent;

import java.time.Instant;

public record AgentAuditEventResponse(
        long sequence,
        String eventType,
        String actorId,
        String detail,
        Instant occurredAt
) {
    static AgentAuditEventResponse from(AgentAuditEvent event) {
        return new AgentAuditEventResponse(
                event.sequence(), event.eventType(), event.actorId(), event.detail(), event.occurredAt());
    }
}
