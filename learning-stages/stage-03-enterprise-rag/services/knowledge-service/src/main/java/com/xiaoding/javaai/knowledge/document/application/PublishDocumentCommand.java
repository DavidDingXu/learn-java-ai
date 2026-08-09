package com.xiaoding.javaai.knowledge.document.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.ActorId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;

import java.time.Instant;
import java.util.List;

public record PublishDocumentCommand(
        TenantId tenantId,
        DocumentId documentId,
        ActorId actorId,
        int versionNumber,
        long expectedRevision,
        Instant effectiveFrom,
        Instant effectiveUntil,
        List<DocumentAclGrant> acl
) {
    public PublishDocumentCommand {
        if (tenantId == null) throw new IllegalArgumentException("tenantId must not be null");
        if (documentId == null) throw new IllegalArgumentException("documentId must not be null");
        if (actorId == null) throw new IllegalArgumentException("actorId must not be null");
        if (versionNumber < 1) throw new IllegalArgumentException("versionNumber must be positive");
        if (expectedRevision < 0) throw new IllegalArgumentException("expectedRevision must not be negative");
        if (effectiveFrom == null) throw new IllegalArgumentException("effectiveFrom must not be null");
        if (acl == null || acl.isEmpty()) throw new IllegalArgumentException("acl must not be empty");
        if (acl.size() > 100) throw new IllegalArgumentException("acl must contain at most 100 entries");
        acl = acl.stream().distinct().toList();
        if (acl.stream().anyMatch(grant -> grant.subjectType() == DocumentAclSubjectType.TENANT
                && !grant.subjectId().equals(tenantId.value()))) {
            throw new IllegalArgumentException("TENANT ACL subject must match the token tenant");
        }
    }
}
