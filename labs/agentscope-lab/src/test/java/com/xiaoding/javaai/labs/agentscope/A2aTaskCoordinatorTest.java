package com.xiaoding.javaai.labs.agentscope;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2aTaskCoordinatorTest {

    @Test
    void keepsSubmissionIdempotentAndCallbacksMonotonic() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();
        A2aTaskRequest request = request("tenant-a", "idem-1", "T-1", "分析 T-1");

        A2aTask first = coordinator.submit(request, Instant.parse("2026-07-13T12:00:00Z"));
        A2aTask duplicate = coordinator.submit(request, Instant.parse("2026-07-13T12:00:01Z"));
        A2aTask completed = coordinator.onCallback(first.taskId(), A2aTaskStatus.COMPLETED, "receipt-1");
        A2aTask duplicateCallback = coordinator.onCallback(
                first.taskId(), A2aTaskStatus.COMPLETED, "receipt-1");

        assertEquals(first.taskId(), duplicate.taskId());
        assertEquals(A2aTaskStatus.COMPLETED, completed.status());
        assertSame(completed, duplicateCallback);
        assertThrows(IllegalStateException.class,
                () -> coordinator.onCallback(first.taskId(), A2aTaskStatus.RUNNING, "late"));
        assertThrows(IllegalStateException.class,
                () -> coordinator.markDeliveryUnknown(request.tenantId(), request.idempotencyKey()));

        IllegalStateException changedReceipt = assertThrows(IllegalStateException.class,
                () -> coordinator.onCallback(first.taskId(), A2aTaskStatus.COMPLETED, "receipt-2"));
        assertTrue(changedReceipt.getMessage().contains("protocol conflict"));
        IllegalStateException changedTerminalState = assertThrows(IllegalStateException.class,
                () -> coordinator.onCallback(first.taskId(), A2aTaskStatus.FAILED, "receipt-1"));
        assertTrue(changedTerminalState.getMessage().contains("protocol conflict"));
    }

    @Test
    void conflictingIdempotencyAndTimeoutAreHandledExplicitly() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();
        coordinator.submit(request("tenant-a", "idem-2", "T-2", "分析 T-2"), Instant.now());

        assertThrows(IllegalArgumentException.class, () -> coordinator.submit(
                request("tenant-a", "idem-2", "T-3", "分析 T-3"), Instant.now()));
        A2aTask unknown = coordinator.markDeliveryUnknown("tenant-a", "idem-2");
        assertEquals(A2aTaskStatus.UNKNOWN, unknown.status());
        assertNotNull(AgentScopeA2aClientBoundary.defaultConfig());
    }

    @Test
    void computesTheFingerprintServerSideAndNamespacesKeysByTenant() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();
        A2aTaskRequest firstRequest = request("tenant-a", "idem-shared", "T-9", "分析  T-9\n的风险");
        A2aTaskRequest normalizedDuplicate = request("tenant-a", "idem-shared", "T-9", "分析 T-9 的风险");
        A2aTaskRequest anotherTenant = request("tenant-b", "idem-shared", "T-9", "分析 T-9 的风险");

        A2aTask first = coordinator.submit(firstRequest, Instant.now());
        A2aTask duplicate = coordinator.submit(normalizedDuplicate, Instant.now());
        A2aTask isolated = coordinator.submit(anotherTenant, Instant.now());

        assertEquals(firstRequest.requestFingerprint(), normalizedDuplicate.requestFingerprint());
        assertEquals(first.taskId(), duplicate.taskId());
        assertTrue(!first.taskId().equals(isolated.taskId()));
        assertTrue(!firstRequest.requestFingerprint().equals(anotherTenant.requestFingerprint()));
    }

    @Test
    void keepsFieldBoundariesUnambiguousInTheRequestFingerprint() {
        A2aTaskRequest first = new A2aTaskRequest(
                "tenant-a", "idem-boundary", "risk", "agent-review",
                "T-9", "ticket:risk-summary", "分析 T-9", "v1");
        A2aTaskRequest second = new A2aTaskRequest(
                "tenant-a", "idem-boundary", "riska", "gent-review",
                "T-9", "ticket:risk-summary", "分析 T-9", "v1");

        assertNotEquals(first.requestFingerprint(), second.requestFingerprint());
    }

    @Test
    void acceptsAConfirmedTerminalResultAfterUnknownDelivery() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();
        A2aTask submitted = coordinator.submit(
                request("tenant-a", "idem-3", "T-3", "分析 T-3"), Instant.now());

        coordinator.markDeliveryUnknown("tenant-a", "idem-3");
        A2aTask completed = coordinator.onCallback(
                submitted.taskId(), A2aTaskStatus.COMPLETED, "receipt-3");

        assertEquals(A2aTaskStatus.COMPLETED, completed.status());
        assertEquals("receipt-3", completed.remoteReceipt());
    }

    @Test
    void rejectsCallbacksForUnknownTasks() {
        A2aTaskCoordinator coordinator = new A2aTaskCoordinator();

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> coordinator.onCallback("missing-task", A2aTaskStatus.COMPLETED, "receipt"));

        assertTrue(error.getMessage().contains("unknown A2A task"));
    }

    private static A2aTaskRequest request(
            String tenantId,
            String idempotencyKey,
            String businessObjectId,
            String instruction
    ) {
        return new A2aTaskRequest(
                tenantId,
                idempotencyKey,
                "risk-agent",
                "risk-review",
                businessObjectId,
                "ticket:risk-summary",
                instruction,
                "v1");
    }
}
