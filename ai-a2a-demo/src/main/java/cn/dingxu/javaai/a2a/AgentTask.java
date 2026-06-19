package cn.dingxu.javaai.a2a;

import java.time.Instant;
import java.util.List;

public record AgentTask(
        String taskId,
        String skillId,
        TaskState state,
        String statusMessage,
        List<AgentArtifact> artifacts,
        Instant createdAt,
        Instant updatedAt
) {
    public AgentTask {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (skillId == null || skillId.isBlank()) {
            throw new IllegalArgumentException("skillId must not be blank");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
        statusMessage = statusMessage == null ? "" : statusMessage;
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static AgentTask completed(String taskId, String skillId, String message, List<AgentArtifact> artifacts) {
        Instant now = Instant.now();
        return new AgentTask(taskId, skillId, TaskState.COMPLETED, message, artifacts, now, now);
    }

    public static AgentTask inputRequired(String taskId, String skillId, String message) {
        Instant now = Instant.now();
        return new AgentTask(taskId, skillId, TaskState.INPUT_REQUIRED, message, List.of(), now, now);
    }

    public static AgentTask failed(String taskId, String skillId, String message) {
        Instant now = Instant.now();
        return new AgentTask(taskId, skillId, TaskState.FAILED, message, List.of(), now, now);
    }
}
