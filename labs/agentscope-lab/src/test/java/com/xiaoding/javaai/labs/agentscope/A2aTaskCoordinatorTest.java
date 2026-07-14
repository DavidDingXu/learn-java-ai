package com.xiaoding.javaai.labs.agentscope;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class A2aTaskCoordinatorTest {

    @Test
    void keepsSubmissionIdempotentAndCallbacksMonotonic() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();
        A2aTaskRequest request = new A2aTaskRequest("idem-1", "risk-agent", "分析 T-1", "hash-a");

        A2aTask first = coordinator.submit(request, Instant.parse("2026-07-13T12:00:00Z"));
        A2aTask duplicate = coordinator.submit(request, Instant.parse("2026-07-13T12:00:01Z"));
        A2aTask completed = coordinator.onCallback(first.taskId(), A2aTaskStatus.COMPLETED, "receipt-1");

        assertEquals(first.taskId(), duplicate.taskId());
        assertEquals(A2aTaskStatus.COMPLETED, completed.status());
        assertThrows(IllegalStateException.class,
                () -> coordinator.onCallback(first.taskId(), A2aTaskStatus.RUNNING, "late"));
        assertThrows(IllegalStateException.class,
                () -> coordinator.markDeliveryUnknown(request.idempotencyKey()));
    }

    @Test
    void conflictingIdempotencyAndTimeoutAreHandledExplicitly() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();
        coordinator.submit(new A2aTaskRequest("idem-2", "risk-agent", "分析 T-2", "hash-a"), Instant.now());

        assertThrows(IllegalArgumentException.class, () -> coordinator.submit(
                new A2aTaskRequest("idem-2", "risk-agent", "分析 T-3", "hash-b"), Instant.now()));
        A2aTask unknown = coordinator.markDeliveryUnknown("idem-2");
        assertEquals(A2aTaskStatus.UNKNOWN, unknown.status());
        assertNotNull(AgentScopeA2aClientBoundary.defaultConfig());
    }
}
