package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAgentTaskRepositoryTest {

    @Test
    void rejects_a_stale_task_version_instead_of_losing_a_concurrent_transition() {
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        DelegatedTicketIdentity identity = new DelegatedTicketIdentity(
                "tenant-a", "customer-42", "customer-bff", List.of(), List.of());
        AgentTaskRepository.TaskAcceptance acceptance = repository.accept(
                identity, "handoff-key", "fingerprint", () -> AgentTask.accepted(
                        "task-100", identity,
                        new AgentTaskRequest("case-1", "resolve", Map.of()),
                        Instant.parse("2026-07-13T08:00:00Z")));

        AgentTask firstUpdate = acceptance.task().start(Instant.parse("2026-07-13T08:01:00Z"));
        repository.save(firstUpdate, 0);
        AgentTask staleUpdate = acceptance.task().start(Instant.parse("2026-07-13T08:01:01Z"));

        assertThatThrownBy(() -> repository.save(staleUpdate, 0))
                .isInstanceOf(OptimisticTaskLockException.class);
    }
}
