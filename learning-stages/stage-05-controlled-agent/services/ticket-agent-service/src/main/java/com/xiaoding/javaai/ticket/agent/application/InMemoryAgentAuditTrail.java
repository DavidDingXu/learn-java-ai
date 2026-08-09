package com.xiaoding.javaai.ticket.agent.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemoryAgentAuditTrail implements AgentAuditTrail {

    private final ConcurrentMap<String, List<AgentAuditEvent>> eventsByTask = new ConcurrentHashMap<>();

    @Override
    public AgentAuditEvent append(
            String taskId,
            String eventType,
            String actorId,
            String detail,
            Instant occurredAt
    ) {
        final AgentAuditEvent[] appended = new AgentAuditEvent[1];
        eventsByTask.compute(taskId, (ignored, existing) -> {
            ArrayList<AgentAuditEvent> updated = existing == null
                    ? new ArrayList<>() : new ArrayList<>(existing);
            AgentAuditEvent event = new AgentAuditEvent(
                    taskId, updated.size() + 1L, eventType, actorId, detail, occurredAt);
            updated.add(event);
            appended[0] = event;
            return List.copyOf(updated);
        });
        return appended[0];
    }

    @Override
    public List<AgentAuditEvent> findByTaskId(String taskId) {
        return eventsByTask.getOrDefault(taskId, List.of());
    }
}
