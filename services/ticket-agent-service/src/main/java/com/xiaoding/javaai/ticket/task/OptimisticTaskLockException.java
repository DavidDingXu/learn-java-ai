package com.xiaoding.javaai.ticket.task;

public final class OptimisticTaskLockException extends RuntimeException {
    public OptimisticTaskLockException(String taskId, long expectedVersion, long actualVersion) {
        super("agent task version conflict: taskId=" + taskId
                + ", expected=" + expectedVersion + ", actual=" + actualVersion);
    }
}
