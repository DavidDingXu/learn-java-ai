package com.xiaoding.javaai.ticket.agent.application;

import java.time.Instant;

public record AgentAuditEvent(
        String taskId,
        long sequence,
        String eventType,
        String actorId,
        String detail,
        Instant occurredAt
) {
}
