package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public final class IndexTaskWorker {

    private final String workerId;
    private final IndexTaskQueue taskQueue;
    private final DocumentVersionIndexer indexer;
    private final Supplier<Instant> clock;
    private final Duration leaseDuration;
    private final Duration retryDelay;
    private final int maximumAttempts;

    public IndexTaskWorker(
            String workerId,
            IndexTaskQueue taskQueue,
            DocumentVersionIndexer indexer,
            Supplier<Instant> clock,
            Duration leaseDuration,
            Duration retryDelay,
            int maximumAttempts
    ) {
        if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId must not be blank");
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        if (retryDelay == null || retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must not be negative");
        }
        if (maximumAttempts < 1) throw new IllegalArgumentException("maximumAttempts must be positive");
        this.workerId = workerId.strip();
        this.taskQueue = taskQueue;
        this.indexer = indexer;
        this.clock = clock;
        this.leaseDuration = leaseDuration;
        this.retryDelay = retryDelay;
        this.maximumAttempts = maximumAttempts;
    }

    public IndexTaskRunResult runOnce() {
        Instant claimedAt = clock.get();
        var claimed = taskQueue.claimNext(workerId, claimedAt, leaseDuration, maximumAttempts);
        if (claimed.isEmpty()) return IndexTaskRunResult.IDLE;

        ClaimedIndexTask task = claimed.orElseThrow();
        try {
            indexer.index(task);
            taskQueue.complete(task.taskId(), workerId, task.leaseAttempt(), clock.get());
            return IndexTaskRunResult.SUCCEEDED;
        } catch (IndexingException error) {
            taskQueue.fail(
                    task.taskId(),
                    workerId,
                    task.leaseAttempt(),
                    clock.get(),
                    error.errorCode(),
                    retryDelay,
                    maximumAttempts
            );
            return IndexTaskRunResult.FAILED;
        }
    }
}
