package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTaskIntakeServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-13T08:00:00Z"), ZoneOffset.UTC);

    @Test
    void returns_the_original_task_for_the_same_idempotency_key_and_payload() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("task-100", "task-101"));
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        AgentTaskIntakeService service = new AgentTaskIntakeService(ids::removeFirst, repository, CLOCK);
        AgentTaskRequest request = request("退款多久到账？");

        AgentTaskReceipt first = service.accept(identity("tenant-a"),
                "handoff:tenant-a:conversation-1:attempt-1", request);
        AgentTaskReceipt duplicate = service.accept(identity("tenant-a"),
                "handoff:tenant-a:conversation-1:attempt-1", request);

        assertThat(first.taskId()).isEqualTo("task-100");
        assertThat(first.duplicate()).isFalse();
        assertThat(duplicate.taskId()).isEqualTo("task-100");
        assertThat(duplicate.duplicate()).isTrue();
    }

    @Test
    void rejects_reusing_an_idempotency_key_for_different_business_input() {
        AgentTaskIntakeService service = new AgentTaskIntakeService(
                () -> "task-100", new InMemoryAgentTaskRepository(), CLOCK);
        String key = "handoff:tenant-a:conversation-1:attempt-1";
        service.accept(identity("tenant-a"), key, request("退款多久到账？"));

        assertThatThrownBy(() -> service.accept(identity("tenant-a"), key, request("订单为什么被取消？")))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void rejects_different_requests_even_when_delimiter_joining_would_look_identical() {
        AgentTaskIntakeService service = new AgentTaskIntakeService(
                () -> "task-100", new InMemoryAgentTaskRepository(), CLOCK);
        String key = "handoff:tenant-a:collision-check";
        AgentTaskRequest first = new AgentTaskRequest("case\nsegment", "objective", Map.of());
        AgentTaskRequest second = new AgentTaskRequest("case", "segment\nobjective", Map.of());
        service.accept(identity("tenant-a"), key, first);

        assertThatThrownBy(() -> service.accept(identity("tenant-a"), key, second))
                .isInstanceOf(IdempotencyConflictException.class);
    }

    @Test
    void scopes_idempotency_and_task_ownership_by_the_trusted_tenant() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("task-a", "task-b"));
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        AgentTaskIntakeService service = new AgentTaskIntakeService(ids::removeFirst, repository, CLOCK);
        String sameExternalKey = "handoff:shared-key";

        AgentTaskReceipt tenantA = service.accept(identity("tenant-a"), sameExternalKey, request("退款多久到账？"));
        AgentTaskReceipt tenantB = service.accept(identity("tenant-b"), sameExternalKey, request("退款多久到账？"));

        assertThat(tenantA.taskId()).isEqualTo("task-a");
        assertThat(tenantB.taskId()).isEqualTo("task-b");
        assertThat(repository.findById("task-a").orElseThrow().identity().tenantId())
                .isEqualTo("tenant-a");
        assertThat(repository.findById("task-a").orElseThrow().identity().subjectId())
                .isEqualTo("customer-42");
    }

    @Test
    void scopes_idempotency_by_the_trusted_subject_inside_the_same_tenant() {
        ArrayDeque<String> ids = new ArrayDeque<>(List.of("task-a", "task-b"));
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        AgentTaskIntakeService service = new AgentTaskIntakeService(ids::removeFirst, repository, CLOCK);
        String sameExternalKey = "handoff:shared-key";

        AgentTaskReceipt customerA = service.accept(
                identity("tenant-a", "customer-42"), sameExternalKey, request("退款多久到账？"));
        AgentTaskReceipt customerB = service.accept(
                identity("tenant-a", "customer-43"), sameExternalKey, request("退款多久到账？"));

        assertThat(customerA.taskId()).isEqualTo("task-a");
        assertThat(customerB.taskId()).isEqualTo("task-b");
        assertThat(repository.findById("task-b").orElseThrow().identity().subjectId())
                .isEqualTo("customer-43");
    }

    @Test
    void keeps_the_acceptance_receipt_stable_after_the_task_has_started() {
        InMemoryAgentTaskRepository repository = new InMemoryAgentTaskRepository();
        AgentTaskIntakeService service = new AgentTaskIntakeService(
                () -> "task-100", repository, CLOCK);
        String key = "handoff:tenant-a:conversation-1:attempt-1";
        DelegatedTicketIdentity identity = identity("tenant-a");
        AgentTaskRequest request = request("退款多久到账？");
        AgentTaskReceipt first = service.accept(identity, key, request);
        AgentTask accepted = repository.findById(first.taskId()).orElseThrow();
        repository.save(accepted.start(Instant.parse("2026-07-13T08:01:00Z")), accepted.version());

        AgentTaskReceipt duplicate = service.accept(identity, key, request);

        assertThat(duplicate.status()).isEqualTo("ACCEPTED");
        assertThat(duplicate.duplicate()).isTrue();
        assertThat(repository.findById(first.taskId()).orElseThrow().status()).isEqualTo("RUNNING");
    }

    private static AgentTaskRequest request(String question) {
        return new AgentTaskRequest(
                "conversation-1",
                "Resolve customer consultation: " + question,
                Map.of("question", question, "sourceTraceId", "trace-123")
        );
    }

    private static DelegatedTicketIdentity identity(String tenantId) {
        return identity(tenantId, "customer-42");
    }

    private static DelegatedTicketIdentity identity(String tenantId, String subjectId) {
        return new DelegatedTicketIdentity(
                tenantId, subjectId, "customer-bff", List.of("CUSTOMER"), List.of("retail"));
    }
}
