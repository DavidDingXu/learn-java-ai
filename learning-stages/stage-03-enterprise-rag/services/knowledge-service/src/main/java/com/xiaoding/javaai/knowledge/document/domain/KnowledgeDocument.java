package com.xiaoding.javaai.knowledge.document.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

public final class KnowledgeDocument {

    private final DocumentId id;
    private final TenantId tenantId;
    private final String title;
    private final ActorId createdBy;
    private final Instant createdAt;
    private final Map<Integer, DocumentVersion> versions = new LinkedHashMap<>();
    private long revision;

    private KnowledgeDocument(
            DocumentId id,
            TenantId tenantId,
            String title,
            ActorId createdBy,
            Instant createdAt
    ) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("title must not be blank");
        this.id = id;
        this.tenantId = tenantId;
        this.title = title;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public static KnowledgeDocument create(
            DocumentId id,
            TenantId tenantId,
            String title,
            ActorId createdBy,
            Instant createdAt
    ) {
        return new KnowledgeDocument(id, tenantId, title, createdBy, createdAt);
    }

    public static KnowledgeDocument restore(
            DocumentId id,
            TenantId tenantId,
            String title,
            ActorId createdBy,
            Instant createdAt,
            long revision,
            List<DocumentVersion> versions
    ) {
        if (revision < 0) throw new IllegalArgumentException("revision must not be negative");
        KnowledgeDocument document = new KnowledgeDocument(id, tenantId, title, createdBy, createdAt);
        for (DocumentVersion version : versions) {
            if (document.versions.put(version.number(), version) != null) {
                throw new IllegalArgumentException("duplicate document version " + version.number());
            }
        }
        document.revision = revision;
        return document;
    }

    public DocumentVersion addVersion(
            long expectedRevision,
            ContentHash contentHash,
            ObjectKey objectKey,
            String mediaType,
            ActorId actorId,
            Instant now
    ) {
        checkRevision(expectedRevision);
        if (versions.values().stream().anyMatch(version -> version.contentHash().equals(contentHash))) {
            throw new DuplicateDocumentContentException(contentHash);
        }
        DocumentVersion version = new DocumentVersion(
                versions.size() + 1, contentHash, objectKey, mediaType, actorId, now
        );
        versions.put(version.number(), version);
        revision += 1;
        return version;
    }

    public void publishVersion(
            long expectedRevision,
            int versionNumber,
            Instant effectiveFrom,
            Instant effectiveUntil
    ) {
        checkRevision(expectedRevision);
        if (effectiveFrom == null) throw new IllegalArgumentException("effectiveFrom must not be null");
        if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
            throw new InvalidDocumentVersionWindowException();
        }
        DocumentVersion target = version(versionNumber);
        if (target.status() != DocumentVersionStatus.DRAFT) {
            throw new IllegalStateException("only a draft version can be published");
        }
        publishedVersion().ifPresent(current -> current.retire(effectiveFrom));
        target.publish(effectiveFrom, effectiveUntil);
        revision += 1;
    }

    private void checkRevision(long expectedRevision) {
        if (expectedRevision != revision) throw new DocumentRevisionConflictException(expectedRevision, revision);
    }

    public DocumentVersion version(int number) {
        DocumentVersion version = versions.get(number);
        if (version == null) throw new IllegalArgumentException("unknown document version " + number);
        return version;
    }

    public Optional<DocumentVersion> publishedVersion() {
        return versions.values().stream()
                .filter(version -> version.status() == DocumentVersionStatus.PUBLISHED)
                .findFirst();
    }

    public List<DocumentVersion> versions() {
        return List.copyOf(versions.values());
    }

    public DocumentId id() {
        return id;
    }

    public TenantId tenantId() {
        return tenantId;
    }

    public String title() {
        return title;
    }

    public ActorId createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public long revision() {
        return revision;
    }
}
