package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public final class InMemoryAgentTaskRepository implements AgentTaskRepository {

    private final ConcurrentMap<String, StoredTask> tasksByScopedIdempotencyKey = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AgentTask> tasksById = new ConcurrentHashMap<>();

    @Override
    public TaskAcceptance accept(
            DelegatedTicketIdentity identity,
            String idempotencyKey,
            String fingerprint,
            Supplier<AgentTask> newTask
    ) {
        String scopedKey = String.join("\n",
                identity.tenantId(), identity.subjectId(), identity.actorId(), idempotencyKey);
        AtomicReference<TaskAcceptance> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        tasksByScopedIdempotencyKey.compute(scopedKey, (ignored, existing) -> {
            if (existing == null) {
                AgentTask task = newTask.get();
                tasksById.put(task.taskId(), task);
                result.set(new TaskAcceptance(task, false));
                return new StoredTask(fingerprint, task.taskId());
            }
            if (!existing.fingerprint().equals(fingerprint)) {
                failure.set(new IdempotencyConflictException(idempotencyKey));
                return existing;
            }
            result.set(new TaskAcceptance(tasksById.get(existing.taskId()), true));
            return existing;
        });
        if (failure.get() != null) throw failure.get();
        return result.get();
    }

    @Override
    public Optional<AgentTask> findById(String taskId) {
        return Optional.ofNullable(tasksById.get(taskId));
    }

    @Override
    public AgentTask save(AgentTask task, long expectedVersion) {
        AtomicReference<AgentTask> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        tasksById.compute(task.taskId(), (ignored, existing) -> {
            if (existing == null) {
                failure.set(new AgentTaskNotFoundException(task.taskId()));
                return null;
            }
            if (existing.version() != expectedVersion) {
                failure.set(new OptimisticTaskLockException(
                        task.taskId(), expectedVersion, existing.version()));
                return existing;
            }
            result.set(task);
            return task;
        });
        if (failure.get() != null) throw failure.get();
        return result.get();
    }

    private record StoredTask(String fingerprint, String taskId) {
    }
}
