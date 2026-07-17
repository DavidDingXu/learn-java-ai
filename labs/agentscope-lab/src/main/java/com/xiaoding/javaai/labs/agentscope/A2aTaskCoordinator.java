package com.xiaoding.javaai.labs.agentscope;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class A2aTaskCoordinator {

    private final Map<String, A2aTask> byIdempotencyKey = new ConcurrentHashMap<>();
    private final Map<String, A2aTask> byTaskId = new ConcurrentHashMap<>();

    public synchronized A2aTask submit(A2aTaskRequest request, Instant now) {
        A2aTask existing = byIdempotencyKey.get(request.idempotencyKey());
        if (existing != null) {
            if (!existing.request().requestHash().equals(request.requestHash())) {
                throw new IllegalArgumentException("idempotency key was reused with another payload");
            }
            return existing;
        }
        String taskId = UUID.nameUUIDFromBytes(
                (request.idempotencyKey() + ":" + request.requestHash()).getBytes(StandardCharsets.UTF_8)).toString();
        A2aTask task = new A2aTask(taskId, request, A2aTaskStatus.SUBMITTED, null, now);
        byIdempotencyKey.put(request.idempotencyKey(), task);
        byTaskId.put(taskId, task);
        return task;
    }

    public synchronized A2aTask onCallback(String taskId, A2aTaskStatus status, String remoteReceipt) {
        A2aTask current = requireTask(taskId);
        if (isTerminal(current.status()) && isTerminal(status)) {
            if (current.status() == status && Objects.equals(current.remoteReceipt(), remoteReceipt)) {
                return current;
            }
            throw new IllegalStateException(
                    "A2A protocol conflict for terminal task " + taskId
                            + ": existing=" + current.status() + ", incoming=" + status);
        }
        if (!canTransition(current.status(), status)) {
            throw new IllegalStateException("illegal A2A task transition: " + current.status() + " -> " + status);
        }
        A2aTask updated = current.withStatus(status, remoteReceipt);
        replace(updated);
        return updated;
    }

    public synchronized A2aTask markDeliveryUnknown(String idempotencyKey) {
        A2aTask current = byIdempotencyKey.get(idempotencyKey);
        if (current == null) {
            throw new IllegalArgumentException("unknown idempotency key: " + idempotencyKey);
        }
        if (!canTransition(current.status(), A2aTaskStatus.UNKNOWN)) {
            throw new IllegalStateException(
                    "illegal A2A task transition: " + current.status() + " -> " + A2aTaskStatus.UNKNOWN);
        }
        A2aTask updated = current.withStatus(A2aTaskStatus.UNKNOWN, null);
        replace(updated);
        return updated;
    }

    private A2aTask requireTask(String taskId) {
        A2aTask task = byTaskId.get(taskId);
        if (task == null) {
            throw new IllegalArgumentException("unknown A2A task: " + taskId);
        }
        return task;
    }

    private void replace(A2aTask task) {
        byTaskId.put(task.taskId(), task);
        byIdempotencyKey.put(task.request().idempotencyKey(), task);
    }

    private static boolean canTransition(A2aTaskStatus current, A2aTaskStatus next) {
        if (current == next) {
            return true;
        }
        return switch (current) {
            case SUBMITTED -> next == A2aTaskStatus.RUNNING || next == A2aTaskStatus.COMPLETED
                    || next == A2aTaskStatus.FAILED || next == A2aTaskStatus.UNKNOWN;
            case RUNNING -> next == A2aTaskStatus.COMPLETED || next == A2aTaskStatus.FAILED
                    || next == A2aTaskStatus.UNKNOWN;
            case UNKNOWN -> next == A2aTaskStatus.COMPLETED || next == A2aTaskStatus.FAILED;
            case COMPLETED, FAILED -> false;
        };
    }

    private static boolean isTerminal(A2aTaskStatus status) {
        return status == A2aTaskStatus.COMPLETED || status == A2aTaskStatus.FAILED;
    }
}
