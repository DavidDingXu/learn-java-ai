package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PostgresIndexTaskMutationQueryTest {

    private static final UUID TASK_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final Instant NOW = Instant.parse("2026-07-13T07:00:00Z");

    @Test
    void completion_requires_the_current_owner_attempt_and_unexpired_lease() {
        PostgresIndexTaskMutationQuery query = PostgresIndexTaskMutationQuery.complete(
                TASK_ID, "indexer-a", 2, NOW
        );

        String sql = query.sql().toLowerCase();
        assertThat(sql).contains("status = 'running'");
        assertThat(sql).contains("lease_owner = ?");
        assertThat(sql).contains("attempts = ?");
        assertThat(sql).contains("lease_until > ?");
        assertThat(query.parameters()).containsExactly(NOW, TASK_ID, "indexer-a", 2, NOW);
    }

    @Test
    void failure_uses_the_attempt_budget_to_choose_retry_or_dead() {
        PostgresIndexTaskMutationQuery query = PostgresIndexTaskMutationQuery.fail(
                TASK_ID,
                "indexer-a",
                2,
                NOW,
                "EMBEDDING_TIMEOUT",
                Duration.ofMinutes(2),
                3
        );

        String sql = query.sql().toLowerCase();
        assertThat(sql).contains("case when attempts >= ? then 'dead' else 'retry_wait' end");
        assertThat(sql).contains("lease_owner = ?");
        assertThat(sql).contains("attempts = ?");
        assertThat(query.parameters()).containsExactly(
                3,
                3,
                NOW.plusSeconds(120),
                "EMBEDDING_TIMEOUT",
                NOW,
                TASK_ID,
                "indexer-a",
                2,
                NOW
        );
    }

    @Test
    void renewal_extends_only_the_current_unexpired_lease_attempt() {
        PostgresIndexTaskMutationQuery query = PostgresIndexTaskMutationQuery.renewLease(
                TASK_ID, "indexer-a", 2, NOW, Duration.ofSeconds(45)
        );

        String sql = query.sql().toLowerCase();
        assertThat(sql).contains("set lease_until = ?");
        assertThat(sql).contains("status = 'running'");
        assertThat(sql).contains("lease_owner = ?");
        assertThat(sql).contains("attempts = ?");
        assertThat(sql).contains("lease_until > ?");
        assertThat(query.parameters()).containsExactly(
                NOW.plusSeconds(45), NOW, TASK_ID, "indexer-a", 2, NOW
        );
    }
}
