package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;

import java.util.Optional;
import java.util.function.Supplier;

public interface AgentTaskRepository {

    TaskAcceptance accept(
            DelegatedTicketIdentity identity,
            String idempotencyKey,
            String fingerprint,
            Supplier<AgentTask> newTask
    );

    Optional<AgentTask> findById(String taskId);

    AgentTask save(AgentTask task, long expectedVersion);

    record TaskAcceptance(AgentTask task, boolean duplicate) {
    }
}
