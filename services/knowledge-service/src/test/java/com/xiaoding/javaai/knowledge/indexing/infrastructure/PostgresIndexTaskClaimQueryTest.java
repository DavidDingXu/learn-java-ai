package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresIndexTaskClaimQueryTest {

    @Test
    void claims_one_due_task_atomically_without_blocking_other_workers() {
        Instant now = Instant.parse("2026-07-13T04:00:00Z");

        PostgresIndexTaskClaimQuery query = PostgresIndexTaskClaimQuery.create(
                "indexer-a", now, Duration.ofSeconds(45), 3
        );

        String sql = query.sql().toLowerCase();
        assertThat(sql).contains("for update skip locked");
        assertThat(sql).contains("status in ('pending', 'retry_wait', 'running')");
        assertThat(sql).contains("status = 'dead'");
        assertThat(sql).contains("attempts >= ?");
        assertThat(sql).contains("attempts < ?");
        assertThat(sql).contains("next_attempt_at <= ?");
        assertThat(sql).contains("lease_until <= ?");
        assertThat(sql).contains("attempts = task.attempts + 1");
        assertThat(sql).contains("returning task.*");
        assertThat(query.parameters()).containsExactly(
                now,
                3,
                now,
                3,
                now,
                now,
                "indexer-a",
                now.plusSeconds(45),
                now
        );
    }
}
