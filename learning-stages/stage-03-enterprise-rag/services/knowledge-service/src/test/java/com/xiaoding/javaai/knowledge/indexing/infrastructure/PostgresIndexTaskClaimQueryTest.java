package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;
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

    @Test
    void scopes_both_expired_lease_recovery_and_claim_to_one_tenant() {
        Instant now = Instant.parse("2026-07-13T04:00:00Z");

        PostgresIndexTaskClaimQuery query = PostgresIndexTaskClaimQuery.createForTenant(
                new TenantId("tenant-a"), "indexer-a", now, Duration.ofSeconds(45), 3
        );

        assertThat(query.sql().toLowerCase()).containsSubsequence(
                "where status = 'running'",
                "and attempts >= ?",
                "and lease_until <= ?",
                "and tenant_id = ?",
                "candidate as",
                "where status in ('pending', 'retry_wait', 'running')",
                "and attempts < ?",
                "and tenant_id = ?"
        );
        assertThat(query.parameters()).containsExactly(
                now,
                3,
                now,
                "tenant-a",
                3,
                "tenant-a",
                now,
                now,
                "indexer-a",
                now.plusSeconds(45),
                now
        );
    }
}
