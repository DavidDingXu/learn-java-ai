package cn.dingxu.javaai.enterprise.rag;

import java.time.Instant;

public record IndexTask(
        String taskId,
        String documentId,
        int documentVersion,
        IndexTaskStatus status,
        int chunkCount,
        Instant createdAt
) {
    public IndexTask {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        if (documentVersion < 1) {
            throw new IllegalArgumentException("documentVersion must be positive");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (chunkCount < 0) {
            throw new IllegalArgumentException("chunkCount must not be negative");
        }
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
