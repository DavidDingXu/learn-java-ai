package com.xiaoding.javaai.knowledge.document.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KnowledgeDocumentTest {

    private static final Instant CREATED_AT = Instant.parse("2026-07-12T08:00:00Z");

    @Test
    void publishes_a_new_version_and_retires_the_previous_published_version() {
        KnowledgeDocument document = KnowledgeDocument.create(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                CREATED_AT
        );

        DocumentVersion version1 = document.addVersion(
                0,
                new ContentHash("sha256:v1"),
                new ObjectKey("tenant-a/refund-policy/v1.md"),
                "text/markdown",
                new ActorId("editor-1"),
                CREATED_AT.plusSeconds(60)
        );
        document.publishVersion(1, version1.number(), CREATED_AT.plusSeconds(120), null);

        DocumentVersion version2 = document.addVersion(
                2,
                new ContentHash("sha256:v2"),
                new ObjectKey("tenant-a/refund-policy/v2.md"),
                "text/markdown",
                new ActorId("editor-2"),
                CREATED_AT.plusSeconds(180)
        );
        document.publishVersion(3, version2.number(), CREATED_AT.plusSeconds(240), null);

        assertThat(document.revision()).isEqualTo(4);
        assertThat(document.version(1).status()).isEqualTo(DocumentVersionStatus.RETIRED);
        assertThat(document.version(2).status()).isEqualTo(DocumentVersionStatus.PUBLISHED);
        assertThat(document.publishedVersion().orElseThrow().number()).isEqualTo(2);
    }

    @Test
    void rejects_duplicate_content_in_the_same_document() {
        KnowledgeDocument document = KnowledgeDocument.create(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                CREATED_AT
        );
        document.addVersion(
                0,
                new ContentHash("sha256:same"),
                new ObjectKey("tenant-a/refund-policy/v1.md"),
                "text/markdown",
                new ActorId("editor-1"),
                CREATED_AT.plusSeconds(60)
        );

        assertThatThrownBy(() -> document.addVersion(
                1,
                new ContentHash("sha256:same"),
                new ObjectKey("tenant-a/refund-policy/v2.md"),
                "text/markdown",
                new ActorId("editor-1"),
                CREATED_AT.plusSeconds(120)
        ))
                .isInstanceOf(DuplicateDocumentContentException.class)
                .hasMessageContaining("sha256:same");
    }

    @Test
    void rejects_a_stale_revision_before_mutating_the_aggregate() {
        KnowledgeDocument document = KnowledgeDocument.create(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                CREATED_AT
        );
        document.addVersion(
                0,
                new ContentHash("sha256:v1"),
                new ObjectKey("tenant-a/refund-policy/v1.md"),
                "text/markdown",
                new ActorId("editor-1"),
                CREATED_AT.plusSeconds(60)
        );

        assertThatThrownBy(() -> document.publishVersion(
                0,
                1,
                CREATED_AT.plusSeconds(120),
                null
        ))
                .isInstanceOf(DocumentRevisionConflictException.class)
                .hasMessageContaining("expected revision 0")
                .hasMessageContaining("actual revision 1");

        assertThat(document.version(1).status()).isEqualTo(DocumentVersionStatus.DRAFT);
        assertThat(document.revision()).isEqualTo(1);
    }

    @Test
    void rejects_an_invalid_effective_window() {
        KnowledgeDocument document = KnowledgeDocument.create(
                new DocumentId("refund-policy"),
                new TenantId("tenant-a"),
                "退款政策",
                new ActorId("editor-1"),
                CREATED_AT
        );
        document.addVersion(
                0,
                new ContentHash("sha256:v1"),
                new ObjectKey("tenant-a/refund-policy/v1.md"),
                "text/markdown",
                new ActorId("editor-1"),
                CREATED_AT.plusSeconds(60)
        );

        Instant effectiveFrom = CREATED_AT.plusSeconds(300);
        assertThatThrownBy(() -> document.publishVersion(
                1,
                1,
                effectiveFrom,
                effectiveFrom.minusSeconds(1)
        ))
                .isInstanceOf(InvalidDocumentVersionWindowException.class);
    }
}
