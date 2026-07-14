package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.indexing.application.port.DocumentVersionIndexer;
import com.xiaoding.javaai.knowledge.indexing.application.port.IndexTaskQueue;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IndexTaskWorkerTest {

    private static final Instant NOW = Instant.parse("2026-07-13T05:00:00Z");

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

    private static IndexTaskWorker worker(RecordingQueue queue, RecordingIndexer indexer) {
        return new IndexTaskWorker(
                "indexer-a",
                queue,
                indexer,
                () -> NOW,
                Duration.ofSeconds(45),
                Duration.ofMinutes(2),
                3
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

        @Override
        public void index(ClaimedIndexTask task) {
            indexedTask = task;
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
            return Optional.ofNullable(task);
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
            failedTaskId = taskId;
            failureCode = errorCode;
            this.retryDelay = retryDelay;
            this.maximumAttempts = maximumAttempts;
        }
    }
}
