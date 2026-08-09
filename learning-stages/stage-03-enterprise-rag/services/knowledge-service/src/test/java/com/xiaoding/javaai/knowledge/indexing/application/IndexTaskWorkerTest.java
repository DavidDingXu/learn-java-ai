package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class IndexTaskWorkerTest {

    private static final Instant NOW = Instant.parse("2026-07-13T05:00:00Z");
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon(true).factory()
    );

    @AfterEach
    void stopExecutor() {
        executor.shutdownNow();
    }

    @Test
    void completes_the_claimed_task_after_the_document_version_is_indexed() {
        RecordingQueue queue = new RecordingQueue(task());
        RecordingIndexer indexer = new RecordingIndexer();
        IndexTaskWorker worker = worker(queue, indexer);

        IndexTaskRunResult result = worker.runOnce();

        assertThat(result).isEqualTo(IndexTaskRunResult.SUCCEEDED);
        assertThat(indexer.indexedTask).isEqualTo(task());
        assertThat(queue.completedTaskId).isEqualTo(task().taskId());
        assertThat(queue.failedTaskId).isNull();
    }

    @Test
    void records_a_stable_failure_code_and_retry_policy_when_indexing_fails() {
        RecordingQueue queue = new RecordingQueue(task());
        RecordingIndexer indexer = new RecordingIndexer();
        indexer.failure = new IndexingException("EMBEDDING_TIMEOUT", "embedding provider timed out");

        IndexTaskRunResult result = worker(queue, indexer).runOnce();

        assertThat(result).isEqualTo(IndexTaskRunResult.FAILED);
        assertThat(queue.failedTaskId).isEqualTo(task().taskId());
        assertThat(queue.failureCode).isEqualTo("EMBEDDING_TIMEOUT");
        assertThat(queue.retryDelay).isEqualTo(Duration.ofMinutes(2));
        assertThat(queue.maximumAttempts).isEqualTo(3);
    }

    @Test
    void returns_idle_without_calling_the_indexer_when_no_task_is_due() {
        RecordingQueue queue = new RecordingQueue(null);
        RecordingIndexer indexer = new RecordingIndexer();

        assertThat(worker(queue, indexer).runOnce()).isEqualTo(IndexTaskRunResult.IDLE);
        assertThat(indexer.indexedTask).isNull();
    }

    @Test
    void claims_only_the_authenticated_tenant_for_a_manual_run() {
        RecordingQueue queue = new RecordingQueue(task());

        IndexTaskRunResult result = worker(queue, new RecordingIndexer())
                .runOnce(new TenantId("tenant-a"));

        assertThat(result).isEqualTo(IndexTaskRunResult.SUCCEEDED);
        assertThat(queue.claimedTenant).isEqualTo(new TenantId("tenant-a"));
        assertThat(queue.globalClaimed).isFalse();
    }

    @Test
    void returns_lost_lease_instead_of_failing_again_when_failure_mutation_is_fenced_out() {
        RecordingQueue queue = new RecordingQueue(task());
        queue.loseLeaseOnFailure = true;
        RecordingIndexer indexer = new RecordingIndexer();
        indexer.failure = new IndexingException("INDEX_TASK_LEASE_LOST", "lease expired");

        IndexTaskRunResult result = worker(queue, indexer).runOnce();

        assertThat(result).isEqualTo(IndexTaskRunResult.LOST_LEASE);
        assertThat(queue.failureCalls).isEqualTo(1);
    }

    @Test
    void renews_the_fenced_lease_while_a_long_index_operation_is_running() {
        RecordingQueue queue = new RecordingQueue(task());
        RecordingIndexer indexer = new RecordingIndexer();
        indexer.waitFor = queue.renewed;

        IndexTaskRunResult result = worker(queue, indexer, Duration.ofMillis(30)).runOnce();

        assertThat(result).isEqualTo(IndexTaskRunResult.SUCCEEDED);
        assertThat(queue.renewedTaskId).isEqualTo(task().taskId());
        assertThat(queue.renewedLeaseAttempt).isEqualTo(task().leaseAttempt());
        assertThat(queue.renewedLeaseDuration).isEqualTo(Duration.ofMillis(30));
    }

    @Test
    void stops_without_a_terminal_mutation_when_a_heartbeat_detects_a_lost_lease() {
        RecordingQueue queue = new RecordingQueue(task());
        queue.loseLeaseOnRenewal = true;
        RecordingIndexer indexer = new RecordingIndexer();
        indexer.waitFor = queue.renewalAttempted;

        IndexTaskRunResult result = worker(queue, indexer, Duration.ofMillis(30)).runOnce();

        assertThat(result).isEqualTo(IndexTaskRunResult.LOST_LEASE);
        assertThat(queue.completedTaskId).isNull();
        assertThat(queue.failureCalls).isZero();
    }

    private IndexTaskWorker worker(RecordingQueue queue, RecordingIndexer indexer) {
        return worker(queue, indexer, Duration.ofSeconds(45));
    }

    private IndexTaskWorker worker(
            RecordingQueue queue,
            RecordingIndexer indexer,
            Duration leaseDuration
    ) {
        return new IndexTaskWorker(
                "indexer-a",
                queue,
                indexer,
                () -> NOW,
                leaseDuration,
                Duration.ofMinutes(2),
                3,
                executor
        );
    }

    private static ClaimedIndexTask task() {
        return new ClaimedIndexTask(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                new TenantId("tenant-a"),
                new DocumentId("refund-policy"),
                2,
                IndexTaskType.REINDEX_DOCUMENT_VERSION,
                1
        );
    }

    private static final class RecordingIndexer implements DocumentVersionIndexer {
        private ClaimedIndexTask indexedTask;
        private IndexingException failure;
        private CountDownLatch waitFor;

        @Override
        public void index(ClaimedIndexTask task) {
            indexedTask = task;
            if (waitFor != null) {
                try {
                    if (!waitFor.await(1, TimeUnit.SECONDS)) {
                        throw new AssertionError("lease heartbeat did not run");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("interrupted while waiting for lease heartbeat", error);
                }
            }
            if (failure != null) throw failure;
        }
    }

    private static final class RecordingQueue implements IndexTaskQueue {
        private final ClaimedIndexTask task;
        private UUID completedTaskId;
        private UUID failedTaskId;
        private String failureCode;
        private Duration retryDelay;
        private int maximumAttempts;
        private TenantId claimedTenant;
        private boolean globalClaimed;
        private UUID renewedTaskId;
        private int renewedLeaseAttempt;
        private Duration renewedLeaseDuration;
        private boolean loseLeaseOnRenewal;
        private boolean loseLeaseOnFailure;
        private int failureCalls;
        private final CountDownLatch renewalAttempted = new CountDownLatch(1);
        private final CountDownLatch renewed = new CountDownLatch(1);

        private RecordingQueue(ClaimedIndexTask task) {
            this.task = task;
        }

        @Override
        public Optional<ClaimedIndexTask> claimNext(
                String workerId,
                Instant now,
                Duration leaseDuration,
                int maximumAttempts
        ) {
            globalClaimed = true;
            return Optional.ofNullable(task);
        }

        @Override
        public Optional<ClaimedIndexTask> claimNextForTenant(
                TenantId tenantId,
                String workerId,
                Instant now,
                Duration leaseDuration,
                int maximumAttempts
        ) {
            claimedTenant = tenantId;
            return Optional.ofNullable(task);
        }

        @Override
        public void renewLease(
                UUID taskId,
                String workerId,
                int leaseAttempt,
                Instant now,
                Duration leaseDuration
        ) {
            renewalAttempted.countDown();
            if (loseLeaseOnRenewal) throw leaseLost(taskId, workerId, leaseAttempt);
            renewedTaskId = taskId;
            renewedLeaseAttempt = leaseAttempt;
            renewedLeaseDuration = leaseDuration;
            renewed.countDown();
        }

        @Override
        public void complete(UUID taskId, String workerId, int leaseAttempt, Instant now) {
            completedTaskId = taskId;
        }

        @Override
        public void fail(
                UUID taskId,
                String workerId,
                int leaseAttempt,
                Instant now,
                String errorCode,
                Duration retryDelay,
                int maximumAttempts
        ) {
            failureCalls += 1;
            if (loseLeaseOnFailure) throw leaseLost(taskId, workerId, leaseAttempt);
            failedTaskId = taskId;
            failureCode = errorCode;
            this.retryDelay = retryDelay;
            this.maximumAttempts = maximumAttempts;
        }

        private static IndexTaskLeaseLostException leaseLost(
                UUID taskId,
                String workerId,
                int leaseAttempt
        ) {
            return new IndexTaskLeaseLostException(taskId, workerId, leaseAttempt);
        }
    }
}
