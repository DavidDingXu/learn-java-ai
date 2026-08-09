package com.xiaoding.javaai.knowledge.indexing.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.util.UUID;

public record ClaimedIndexTask(
        UUID taskId,
        TenantId tenantId,
        DocumentId documentId,
        int documentVersion,
        IndexTaskType taskType,
        int leaseAttempt
) {
    public ClaimedIndexTask {
        if (taskId == null) throw new IllegalArgumentException("taskId must not be null");
        if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
        if (documentId == null) throw new IllegalArgumentException("documentId must not be null");
        if (documentVersion < 1) throw new IllegalArgumentException("documentVersion must be positive");
        if (taskType == null) throw new IllegalArgumentException("taskType must not be null");
        if (leaseAttempt < 1) throw new IllegalArgumentException("leaseAttempt must be positive");
    }
}
