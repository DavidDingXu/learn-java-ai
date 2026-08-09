package com.xiaoding.javaai.knowledge.indexing.domain;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndexTaskTest {

    private static final Instant NOW = Instant.parse("2026-07-12T11:00:00Z");

    @Test
    void claims_a_due_task_with_a_bounded_lease() {
        IndexTask task = task();

        task.claim("worker-a", NOW, Duration.ofSeconds(30));

        assertThat(task.status()).isEqualTo(IndexTaskStatus.RUNNING);
        assertThat(task.attempts()).isEqualTo(1);
        assertThat(task.leaseOwner()).isEqualTo("worker-a");
        assertThat(task.leaseUntil()).isEqualTo(NOW.plusSeconds(30));
    }

    @Test
    void recovers_an_expired_running_task_for_another_worker() {
        IndexTask task = task();
        task.claim("worker-a", NOW, Duration.ofSeconds(30));

        task.recoverExpiredLease(NOW.plusSeconds(31));
        task.claim("worker-b", NOW.plusSeconds(31), Duration.ofSeconds(20));

        assertThat(task.status()).isEqualTo(IndexTaskStatus.RUNNING);
        assertThat(task.attempts()).isEqualTo(2);
        assertThat(task.leaseOwner()).isEqualTo("worker-b");
    }

    @Test
    void schedules_a_retry_without_allowing_an_early_claim() {
        IndexTask task = task();
        task.claim("worker-a", NOW, Duration.ofSeconds(30));
        task.fail("worker-a", NOW.plusSeconds(5), "EMBEDDING_TIMEOUT", Duration.ofMinutes(2), 3);

        assertThat(task.status()).isEqualTo(IndexTaskStatus.RETRY_WAIT);
        assertThat(task.nextAttemptAt()).isEqualTo(NOW.plusSeconds(125));
        assertThatThrownBy(() -> task.claim("worker-b", NOW.plusSeconds(60), Duration.ofSeconds(30)))
                .isInstanceOf(IndexTaskNotClaimableException.class);
    }

    @Test
    void marks_poison_input_dead_after_the_attempt_budget_is_exhausted() {
        IndexTask task = task();
        task.claim("worker-a", NOW, Duration.ofSeconds(30));

        task.fail("worker-a", NOW.plusSeconds(1), "UNSUPPORTED_DOCUMENT", Duration.ZERO, 1);

        assertThat(task.status()).isEqualTo(IndexTaskStatus.DEAD);
        assertThat(task.errorCode()).isEqualTo("UNSUPPORTED_DOCUMENT");
        assertThatThrownBy(() -> task.claim("worker-b", NOW.plusSeconds(2), Duration.ofSeconds(30)))
                .isInstanceOf(IndexTaskNotClaimableException.class);
    }

    @Test
    void only_the_lease_owner_can_complete_the_task() {
        IndexTask task = task();
        task.claim("worker-a", NOW, Duration.ofSeconds(30));

        assertThatThrownBy(() -> task.complete("worker-b", NOW.plusSeconds(2)))
                .isInstanceOf(IndexTaskLeaseOwnershipException.class);

        task.complete("worker-a", NOW.plusSeconds(3));
        assertThat(task.status()).isEqualTo(IndexTaskStatus.SUCCEEDED);
        assertThat(task.leaseOwner()).isNull();
    }

    private static IndexTask task() {
        return IndexTask.pending(
                UUID.fromString("00000000-0000-0000-0000-000000000020"),
                new TenantId("tenant-a"),
                new DocumentId("refund-policy"),
                2,
                NOW
        );
    }
}
