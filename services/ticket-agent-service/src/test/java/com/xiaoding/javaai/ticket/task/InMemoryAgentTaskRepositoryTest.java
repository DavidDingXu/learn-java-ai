package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryAgentTaskRepositoryTest {

    @Test
    void scopes_idempotency_by_identity_fields_without_delimiter_collisions() {
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        DelegatedTicketIdentity firstIdentity = new DelegatedTicketIdentity(
                "tenant\ncustomer", "42", "customer-bff", List.of(), List.of());
        DelegatedTicketIdentity secondIdentity = new DelegatedTicketIdentity(
                "tenant", "customer\n42", "customer-bff", List.of(), List.of());

        AgentTaskRepository.TaskAcceptance first = repository.accept(
                firstIdentity, "handoff-key", "fingerprint-a", () -> AgentTask.accepted(
                        "task-100", firstIdentity,
                        new AgentTaskRequest("case-1", "resolve", Map.of()),
                        Instant.parse("2026-07-13T08:00:00Z")));
        AgentTaskRepository.TaskAcceptance second = repository.accept(
                secondIdentity, "handoff-key", "fingerprint-b", () -> AgentTask.accepted(
                        "task-200", secondIdentity,
                        new AgentTaskRequest("case-2", "resolve", Map.of()),
                        Instant.parse("2026-07-13T08:00:00Z")));

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).isFalse();
        assertThat(repository.findById("task-100")).isPresent();
        assertThat(repository.findById("task-200")).isPresent();
    }

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
