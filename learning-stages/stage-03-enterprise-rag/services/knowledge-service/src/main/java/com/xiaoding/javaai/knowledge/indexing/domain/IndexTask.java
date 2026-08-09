package com.xiaoding.javaai.knowledge.indexing.domain;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class IndexTask {

    private final UUID taskId;
    private final TenantId tenantId;
    private final DocumentId documentId;
    private final int documentVersion;
    private final Instant createdAt;
    private IndexTaskStatus status;
    private int attempts;
    private String leaseOwner;
    private Instant leaseUntil;
    private Instant nextAttemptAt;
    private String errorCode;
    private Instant updatedAt;

    private IndexTask(
            UUID taskId,
            TenantId tenantId,
            DocumentId documentId,
            int documentVersion,
            Instant createdAt
    ) {
        if (documentVersion < 1) throw new IllegalArgumentException("documentVersion must be positive");
        this.taskId = taskId;
        this.tenantId = tenantId;
        this.documentId = documentId;
        this.documentVersion = documentVersion;
        this.createdAt = createdAt;
        this.status = IndexTaskStatus.PENDING;
        this.nextAttemptAt = createdAt;
        this.updatedAt = createdAt;
    }

    public static IndexTask pending(
            UUID taskId,
            TenantId tenantId,
            DocumentId documentId,
            int documentVersion,
            Instant createdAt
    ) {
        return new IndexTask(taskId, tenantId, documentId, documentVersion, createdAt);
    }

    public void claim(String workerId, Instant now, Duration leaseDuration) {
        requireWorker(workerId);
        if (leaseDuration == null || leaseDuration.isZero() || leaseDuration.isNegative()) {
            throw new IllegalArgumentException("leaseDuration must be positive");
        }
        if ((status != IndexTaskStatus.PENDING && status != IndexTaskStatus.RETRY_WAIT)
                || now.isBefore(nextAttemptAt)) {
            throw new IndexTaskNotClaimableException(status);
        }
        status = IndexTaskStatus.RUNNING;
        attempts += 1;
        leaseOwner = workerId;
        leaseUntil = now.plus(leaseDuration);
        updatedAt = now;
    }

    public void recoverExpiredLease(Instant now) {
        if (status != IndexTaskStatus.RUNNING || leaseUntil == null || leaseUntil.isAfter(now)) return;
        status = IndexTaskStatus.RETRY_WAIT;
        leaseOwner = null;
        leaseUntil = null;
        nextAttemptAt = now;
        updatedAt = now;
    }

    public void fail(
            String workerId,
            Instant now,
            String errorCode,
            Duration retryDelay,
            int maximumAttempts
    ) {
        requireLeaseOwner(workerId, now);
        if (errorCode == null || errorCode.isBlank()) throw new IllegalArgumentException("errorCode must not be blank");
        if (retryDelay == null || retryDelay.isNegative()) throw new IllegalArgumentException("retryDelay must not be negative");
        if (maximumAttempts < 1) throw new IllegalArgumentException("maximumAttempts must be positive");
        this.errorCode = errorCode;
        leaseOwner = null;
        leaseUntil = null;
        if (attempts >= maximumAttempts) {
            status = IndexTaskStatus.DEAD;
        } else {
            status = IndexTaskStatus.RETRY_WAIT;
            nextAttemptAt = now.plus(retryDelay);
        }
        updatedAt = now;
    }

    public void complete(String workerId, Instant now) {
        requireLeaseOwner(workerId, now);
        status = IndexTaskStatus.SUCCEEDED;
        leaseOwner = null;
        leaseUntil = null;
        errorCode = null;
        updatedAt = now;
    }

    private void requireLeaseOwner(String workerId, Instant now) {
        requireWorker(workerId);
        if (status != IndexTaskStatus.RUNNING || !workerId.equals(leaseOwner)) {
            throw new IndexTaskLeaseOwnershipException(workerId, leaseOwner);
        }
        if (leaseUntil == null || !leaseUntil.isAfter(now)) {
            throw new IndexTaskLeaseOwnershipException(workerId, leaseOwner);
        }
    }

    private static void requireWorker(String workerId) {
        if (workerId == null || workerId.isBlank()) throw new IllegalArgumentException("workerId must not be blank");
    }

    public UUID taskId() {
        return taskId;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public DocumentId documentId() {
        return documentId;
    }

    public int documentVersion() {
        return documentVersion;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public IndexTaskStatus status() {
        return status;
    }

    public int attempts() {
        return attempts;
    }

    public String leaseOwner() {
        return leaseOwner;
    }

    public Instant leaseUntil() {
        return leaseUntil;
    }

    public Instant nextAttemptAt() {
        return nextAttemptAt;
    }

    public String errorCode() {
        return errorCode;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
