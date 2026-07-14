package com.xiaoding.javaai.ticket.agent.application;

import java.time.Instant;
import java.util.List;

public interface AgentAuditTrail {
    AgentAuditEvent append(String taskId, String eventType, String actorId, String detail, Instant occurredAt);

    List<AgentAuditEvent> findByTaskId(String taskId);
}
