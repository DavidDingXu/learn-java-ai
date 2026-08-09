package com.xiaoding.javaai.knowledge.indexing.application;

import java.util.UUID;

public final class IndexTaskLeaseLostException extends RuntimeException {

    public IndexTaskLeaseLostException(UUID taskId, String workerId, int leaseAttempt) {
        super("index task lease was lost: taskId=%s workerId=%s leaseAttempt=%d"
                .formatted(taskId, workerId, leaseAttempt));
    }
}
