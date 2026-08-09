package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class IndexTaskWorker implements IndexTaskRunner {

    private final String workerId;
    private final IndexTaskQueue taskQueue;
    private final DocumentVersionIndexer indexer;
    private final Supplier<Instant> clock;
    private final Duration leaseDuration;
    private final Duration retryDelay;
    private final int maximumAttempts;
    private final ScheduledExecutorService leaseRenewalExecutor;

    public IndexTaskWorker(
            String workerId,
            IndexTaskQueue taskQueue,
            DocumentVersionIndexer indexer,
            Supplier<Instant> clock,
            Duration leaseDuration,
            Duration retryDelay,
            int maximumAttempts,
            ScheduledExecutorService leaseRenewalExecutor
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
        this.leaseRenewalExecutor = leaseRenewalExecutor;
    }

    @Override
    public synchronized IndexTaskRunResult runOnce() {
        Instant claimedAt = clock.get();
        return runClaimed(taskQueue.claimNext(workerId, claimedAt, leaseDuration, maximumAttempts));
    }

    @Override
    public synchronized IndexTaskRunResult runOnce(TenantId tenantId) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
        Instant claimedAt = clock.get();
        return runClaimed(taskQueue.claimNextForTenant(
                tenantId, workerId, claimedAt, leaseDuration, maximumAttempts
        ));
    }

    private IndexTaskRunResult runClaimed(Optional<ClaimedIndexTask> claimed) {
        if (claimed.isEmpty()) return IndexTaskRunResult.IDLE;

        ClaimedIndexTask task = claimed.orElseThrow();
        LeaseHeartbeat heartbeat = new LeaseHeartbeat(task);
        heartbeat.start();
        try {
            indexer.index(task);
            heartbeat.close();
            if (heartbeat.leaseLost()) return IndexTaskRunResult.LOST_LEASE;
            try {
                taskQueue.complete(task.taskId(), workerId, task.leaseAttempt(), clock.get());
                return IndexTaskRunResult.SUCCEEDED;
            } catch (IndexTaskLeaseLostException error) {
                return IndexTaskRunResult.LOST_LEASE;
            }
        } catch (IndexingException error) {
            heartbeat.close();
            if (heartbeat.leaseLost()) return IndexTaskRunResult.LOST_LEASE;
            try {
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
            } catch (IndexTaskLeaseLostException leaseLost) {
                return IndexTaskRunResult.LOST_LEASE;
            }
        } finally {
            heartbeat.close();
        }
    }

    private final class LeaseHeartbeat {
        private final ClaimedIndexTask task;
        private ScheduledFuture<?> future;
        private boolean closed;
        private boolean leaseLost;

        private LeaseHeartbeat(ClaimedIndexTask task) {
            this.task = task;
        }

        private void start() {
            long intervalNanos = Math.max(1, leaseDuration.dividedBy(3).toNanos());
            future = leaseRenewalExecutor.scheduleAtFixedRate(
                    this::renew,
                    intervalNanos,
                    intervalNanos,
                    TimeUnit.NANOSECONDS
            );
        }

        private synchronized void renew() {
            if (closed || leaseLost) return;
            try {
                taskQueue.renewLease(
                        task.taskId(), workerId, task.leaseAttempt(), clock.get(), leaseDuration
                );
            } catch (IndexTaskLeaseLostException error) {
                leaseLost = true;
            } catch (RuntimeException ignored) {
                // A later heartbeat or the fenced terminal mutation determines ownership.
            }
        }

        private synchronized void close() {
            if (closed) return;
            closed = true;
            if (future != null) future.cancel(false);
        }

        private synchronized boolean leaseLost() {
            return leaseLost;
        }
    }
}
