package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReciprocalRankFusionTest {

    @Test
    void ranks_a_chunk_seen_by_both_retrievers_ahead_of_single_channel_candidates() {
        RetrievedKnowledgeChunk semanticFirst = chunk("semantic-only", 0.96d);
        RetrievedKnowledgeChunk shared = chunk("shared", 0.82d);
        RetrievedKnowledgeChunk lexicalFirst = chunk("lexical-only", 18.0d);

        List<RetrievedKnowledgeChunk> fused = new ReciprocalRankFusion(60).fuse(
                List.of(semanticFirst, shared),
                List.of(lexicalFirst, shared),
                3
        );

        assertThat(fused).extracting(RetrievedKnowledgeChunk::chunkId)
                .containsExactly("shared", "lexical-only", "semantic-only");
        assertThat(fused.getFirst().score()).isGreaterThan(fused.get(1).score());
    }

    @Test
    void uses_stable_chunk_id_order_when_fused_scores_are_equal() {
        List<RetrievedKnowledgeChunk> fused = new ReciprocalRankFusion(60).fuse(
                List.of(chunk("chunk-b", 0.9d)),
                List.of(chunk("chunk-a", 10.0d)),
                2
        );

        assertThat(fused).extracting(RetrievedKnowledgeChunk::chunkId)
                .containsExactly("chunk-a", "chunk-b");
    }

    @Test
    void counts_a_chunk_only_once_per_retrieval_channel() {
        List<RetrievedKnowledgeChunk> fused = new ReciprocalRankFusion(60).fuse(
                List.of(chunk("chunk-a", 0.9d), chunk("chunk-a", 0.8d)),
                List.of(),
                2
        );

        assertThat(fused).singleElement().satisfies(chunk ->
                assertThat(chunk.score()).isEqualTo(1.0d / 61.0d));
    }

    private static RetrievedKnowledgeChunk chunk(String chunkId, double score) {
        return new RetrievedKnowledgeChunk(
                chunkId,
                new DocumentId("refund-policy"),
                1,
                List.of("退款政策"),
                "第十条",
                "退款审核通过后原路退回。",
                score
        );
    }
}
