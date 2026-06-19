package cn.dingxu.javaai.a2a;

import java.time.Instant;

public record TaskEvent(String taskId, TaskState state, String message, Instant happenedAt) {
    public TaskEvent {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        message = message == null ? "" : message;
        happenedAt = happenedAt == null ? Instant.now() : happenedAt;
    }

    public static TaskEvent of(String taskId, TaskState state, String message) {
        return new TaskEvent(taskId, state, message, Instant.now());
    }
}
