package cn.dingxu.javaai.rag.service;

import java.time.Instant;

public record IndexTaskResult(
        String taskId,
        String documentId,
        String version,
        IndexTaskStatus status,
        int chunkCount,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt
) {
    public IndexTaskResult {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        version = version == null || version.isBlank() ? "unknown" : version;
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        chunkCount = Math.max(0, chunkCount);
        errorMessage = errorMessage == null ? "" : errorMessage;
        startedAt = startedAt == null ? Instant.EPOCH : startedAt;
        finishedAt = finishedAt == null ? startedAt : finishedAt;
    }
}
