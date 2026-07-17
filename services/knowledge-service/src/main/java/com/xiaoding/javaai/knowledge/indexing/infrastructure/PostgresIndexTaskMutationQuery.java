package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PostgresIndexTaskMutationQuery(String sql, List<Object> parameters) {

    public PostgresIndexTaskMutationQuery {
        parameters = List.copyOf(parameters);
    }

    public static PostgresIndexTaskMutationQuery complete(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now
    ) {
        validateLease(taskId, workerId, leaseAttempt, now);
        return new PostgresIndexTaskMutationQuery(
                """
                UPDATE index_task
                   SET status = 'SUCCEEDED',
                       lease_owner = NULL,
                       lease_until = NULL,
                       error_code = NULL,
                       error_message = NULL,
                       updated_at = ?
                 WHERE task_id = ?
                   AND status = 'RUNNING'
                   AND lease_owner = ?
                   AND attempts = ?
                   AND lease_until > ?
                """,
                List.of(now, taskId, workerId.strip(), leaseAttempt, now)
        );
    }

    public static PostgresIndexTaskMutationQuery renewLease(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now,
            Duration leaseDuration
    ) {
        validateLease(taskId, workerId, leaseAttempt, now);
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        return new PostgresIndexTaskMutationQuery(
                """
                UPDATE index_task
                   SET lease_until = ?,
                       updated_at = ?
                 WHERE task_id = ?
                   AND status = 'RUNNING'
                   AND lease_owner = ?
                   AND attempts = ?
                   AND lease_until > ?
                """,
                List.of(now.plus(leaseDuration), now, taskId, workerId.strip(), leaseAttempt, now)
        );
    }

    public static PostgresIndexTaskMutationQuery fail(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now,
            String errorCode,
            Duration retryDelay,
            int maximumAttempts
    ) {
        validateLease(taskId, workerId, leaseAttempt, now);
        if (errorCode == null || errorCode.isBlank()) throw new IllegalArgumentException("errorCode must not be blank");
        if (retryDelay == null || retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must not be negative");
        }
        if (maximumAttempts < 1) throw new IllegalArgumentException("maximumAttempts must be positive");
        return new PostgresIndexTaskMutationQuery(
                """
                UPDATE index_task
                   SET status = CASE WHEN attempts >= ? THEN 'DEAD' ELSE 'RETRY_WAIT' END,
                       next_attempt_at = CASE WHEN attempts >= ? THEN next_attempt_at ELSE ? END,
                       lease_owner = NULL,
                       lease_until = NULL,
                       error_code = ?,
                       updated_at = ?
                 WHERE task_id = ?
                   AND status = 'RUNNING'
                   AND lease_owner = ?
                   AND attempts = ?
                   AND lease_until > ?
                """,
                List.of(
                        maximumAttempts,
                        maximumAttempts,
                        now.plus(retryDelay),
                        errorCode.strip(),
                        now,
                        taskId,
                        workerId.strip(),
                        leaseAttempt,
                        now
                )
        );
    }

    private static void validateLease(UUID taskId, String workerId, int leaseAttempt, Instant now) {
        if (taskId == null) throw new IllegalArgumentException("taskId must not be null");
        if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId must not be blank");
        if (leaseAttempt < 1) throw new IllegalArgumentException("leaseAttempt must be positive");
        if (now == null) throw new IllegalArgumentException("now must not be null");
    }
}
