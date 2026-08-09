package com.xiaoding.javaai.knowledge.document.domain;

import java.time.Instant;

public final class DocumentVersion {

    private final int number;
    private final ContentHash contentHash;
    private final ObjectKey objectKey;
    private final String mediaType;
    private final ActorId createdBy;
    private final Instant createdAt;
    private DocumentVersionStatus status;
    private Instant effectiveFrom;
    private Instant effectiveUntil;

    DocumentVersion(
            int number,
            ContentHash contentHash,
            ObjectKey objectKey,
            String mediaType,
            ActorId createdBy,
            Instant createdAt
    ) {
        if (number < 1) throw new IllegalArgumentException("version number must be positive");
        if (mediaType == null || mediaType.isBlank()) throw new IllegalArgumentException("mediaType must not be blank");
        this.number = number;
        this.contentHash = contentHash;
        this.objectKey = objectKey;
        this.mediaType = mediaType;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.status = DocumentVersionStatus.DRAFT;
    }

    public static DocumentVersion restore(
            int number,
            ContentHash contentHash,
            ObjectKey objectKey,
            String mediaType,
            ActorId createdBy,
            Instant createdAt,
            DocumentVersionStatus status,
            Instant effectiveFrom,
            Instant effectiveUntil
    ) {
        DocumentVersion version = new DocumentVersion(
                number, contentHash, objectKey, mediaType, createdBy, createdAt
        );
        version.status = status;
        version.effectiveFrom = effectiveFrom;
        version.effectiveUntil = effectiveUntil;
        return version;
    }

    void publish(Instant effectiveFrom, Instant effectiveUntil) {
        this.status = DocumentVersionStatus.PUBLISHED;
        this.effectiveFrom = effectiveFrom;
        this.effectiveUntil = effectiveUntil;
    }

    void retire(Instant effectiveUntil) {
        this.status = DocumentVersionStatus.RETIRED;
        this.effectiveUntil = effectiveUntil;
    }

    public int number() {
        return number;
    }

    public ContentHash contentHash() {
        return contentHash;
    }

    public ObjectKey objectKey() {
        return objectKey;
    }

    public String mediaType() {
        return mediaType;
    }

    public ActorId createdBy() {
        return createdBy;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public DocumentVersionStatus status() {
        return status;
    }

    public Instant effectiveFrom() {
        return effectiveFrom;
    }

    public Instant effectiveUntil() {
        return effectiveUntil;
    }
}
