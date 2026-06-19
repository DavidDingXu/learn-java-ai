package com.xiaoding.javaai.enterprise.rag;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class EvidenceGovernanceServiceTest {

    private final EvidenceGovernanceService service = new EvidenceGovernanceService();

    @Test
    void refusesConflictingEvidenceBeforeItEntersPromptContext() {
        DocumentChunk directRefund = chunk(
                "refund-faq",
                "refund-faq-c1",
                "refund-shipped",
                10,
                null,
                null,
                "发货后退款可以直接退款，无需人工复核。"
        );
        DocumentChunk manualReview = chunk(
                "refund-policy",
                "refund-policy-c1",
                "refund-shipped",
                20,
                null,
                null,
                "发货后退款必须转人工复核，不能直接执行退款。"
        );

        EvidenceGovernanceDecision decision = service.select(
                List.of(directRefund, manualReview),
                Instant.parse("2026-06-08T00:00:00Z"),
                3
        );

        assertThat(decision.status()).isEqualTo(EvidenceGovernanceStatus.CONFLICTED);
        assertThat(decision.selectedEvidence()).isEmpty();
        assertThat(decision.rejectedChunkIds()).containsExactlyInAnyOrder("refund-faq-c1", "refund-policy-c1");
        assertThat(decision.reasons()).contains("conflict_topic:refund-shipped");
    }

    @Test
    void selectsCurrentHigherPriorityEvidenceAndLeavesStaleChunksOut() {
        DocumentChunk expiredPolicy = chunk(
                "refund-policy-2025",
                "refund-policy-2025-c1",
                "refund-shipped",
                100,
                Instant.parse("2025-01-01T00:00:00Z"),
                Instant.parse("2025-12-31T23:59:59Z"),
                "旧制度：发货后退款可以直接退款。"
        );
        DocumentChunk currentPolicy = chunk(
                "refund-policy-2026",
                "refund-policy-2026-c1",
                "refund-shipped",
                50,
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                "现行制度：发货后退款需先核对物流状态。"
        );

        EvidenceGovernanceDecision decision = service.select(
                List.of(expiredPolicy, currentPolicy),
                Instant.parse("2026-06-08T00:00:00Z"),
                3
        );

        assertThat(decision.status()).isEqualTo(EvidenceGovernanceStatus.APPROVED);
        assertThat(decision.selectedEvidence()).extracting(DocumentChunk::chunkId)
                .containsExactly("refund-policy-2026-c1");
        assertThat(decision.rejectedChunkIds()).containsExactly("refund-policy-2025-c1");
        assertThat(decision.reasons()).contains("expired:refund-policy-2025-c1");
    }

    private DocumentChunk chunk(String documentId,
                                String chunkId,
                                String topic,
                                int priority,
                                Instant effectiveFrom,
                                Instant effectiveTo,
                                String content) {
        return new DocumentChunk(
                documentId,
                chunkId,
                "tenant-a",
                Set.of("support"),
                DocumentType.POLICY,
                1,
                1,
                topic,
                priority,
                effectiveFrom,
                effectiveTo,
                content
        );
    }
}
