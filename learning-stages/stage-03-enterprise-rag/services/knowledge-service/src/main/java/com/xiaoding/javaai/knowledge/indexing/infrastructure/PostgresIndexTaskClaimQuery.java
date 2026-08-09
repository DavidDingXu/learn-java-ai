package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record PostgresIndexTaskClaimQuery(String sql, List<Object> parameters) {

    public PostgresIndexTaskClaimQuery {
        parameters = List.copyOf(parameters);
    }

    public static PostgresIndexTaskClaimQuery create(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    ) {
        return create(null, workerId, now, leaseDuration, maximumAttempts);
    }

    public static PostgresIndexTaskClaimQuery createForTenant(
            TenantId tenantId,
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    ) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
        return create(tenantId, workerId, now, leaseDuration, maximumAttempts);
    }

    private static PostgresIndexTaskClaimQuery create(
            TenantId tenantId,
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    ) {
        if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId must not be blank");
        if (now == null) throw new IllegalArgumentException("now must not be null");
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        if (maximumAttempts < 1) throw new IllegalArgumentException("maximumAttempts must be positive");

        String tenantFilter = tenantId == null ? "" : " AND tenant_id = ?";
        String sql = """
                WITH exhausted AS (
                    UPDATE index_task
                       SET status = 'DEAD',
                           lease_owner = NULL,
                           lease_until = NULL,
                           error_code = COALESCE(error_code, 'LEASE_ATTEMPTS_EXHAUSTED'),
                           error_message = COALESCE(error_message, 'worker lease expired after maximum attempts'),
                           updated_at = ?
                     WHERE status = 'RUNNING'
                       AND attempts >= ?
                       AND lease_until <= ?
                       %s
                    RETURNING task_id
                ),
                candidate AS (
                    SELECT task_id
                      FROM index_task
                     WHERE status IN ('PENDING', 'RETRY_WAIT', 'RUNNING')
                       AND attempts < ?
                       %s
                       AND (
                            (status IN ('PENDING', 'RETRY_WAIT') AND next_attempt_at <= ?)
                            OR (status = 'RUNNING' AND lease_until <= ?)
                       )
                     ORDER BY next_attempt_at, created_at, task_id
                     FOR UPDATE SKIP LOCKED
                     LIMIT 1
                )
                UPDATE index_task task
                   SET status = 'RUNNING',
                       attempts = task.attempts + 1,
                       lease_owner = ?,
                       lease_until = ?,
                       error_code = NULL,
                       error_message = NULL,
                       updated_at = ?
                  FROM candidate
                 WHERE task.task_id = candidate.task_id
                RETURNING task.*
                """.formatted(tenantFilter, tenantFilter);
        var parameters = new ArrayList<Object>();
        parameters.add(now);
        parameters.add(maximumAttempts);
        parameters.add(now);
        if (tenantId != null) parameters.add(tenantId.value());
        parameters.add(maximumAttempts);
        if (tenantId != null) parameters.add(tenantId.value());
        parameters.add(now);
        parameters.add(now);
        parameters.add(workerId.strip());
        parameters.add(now.plus(leaseDuration));
        parameters.add(now);
        return new PostgresIndexTaskClaimQuery(
                sql,
                parameters
        );
    }
}
