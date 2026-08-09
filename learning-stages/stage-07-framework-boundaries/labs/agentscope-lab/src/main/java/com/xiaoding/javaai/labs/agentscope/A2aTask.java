package com.xiaoding.javaai.labs.agentscope;

import java.time.Instant;

public record A2aTask(
        String taskId,
        A2aTaskRequest request,
        A2aTaskStatus status,
        String remoteReceipt,
        Instant createdAt) {

    A2aTask withStatus(A2aTaskStatus newStatus, String receipt) {
        return new A2aTask(taskId, request, newStatus, receipt, createdAt);
    }
}
