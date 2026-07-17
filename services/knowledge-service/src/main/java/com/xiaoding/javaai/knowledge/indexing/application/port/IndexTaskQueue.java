package com.xiaoding.javaai.knowledge.indexing.application.port;

import com.xiaoding.javaai.knowledge.indexing.application.ClaimedIndexTask;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface IndexTaskQueue {

    Optional<ClaimedIndexTask> claimNext(
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    );

    Optional<ClaimedIndexTask> claimNextForTenant(
            TenantId tenantId,
            String workerId,
            Instant now,
            Duration leaseDuration,
            int maximumAttempts
    );

    void renewLease(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now,
            Duration leaseDuration
    );

    void complete(UUID taskId, String workerId, int leaseAttempt, Instant now);

    void fail(
            UUID taskId,
            String workerId,
            int leaseAttempt,
            Instant now,
            String errorCode,
            Duration retryDelay,
            int maximumAttempts
    );
}
