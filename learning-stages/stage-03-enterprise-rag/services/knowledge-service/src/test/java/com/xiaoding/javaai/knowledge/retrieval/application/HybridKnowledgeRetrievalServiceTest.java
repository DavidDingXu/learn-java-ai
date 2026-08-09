package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeChunkSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeLexicalSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeQueryRewriter;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeReranker;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HybridKnowledgeRetrievalServiceTest {

    @Test
    void rewrites_searches_both_channels_fuses_and_reranks_with_explicit_candidate_budget() {
        RecordingPorts ports = new RecordingPorts();
        HybridKnowledgeRetrievalService service = ports.service();
        RetrieveKnowledgeQuery query = query(3);

        KnowledgeRetrievalResult result = service.retrieve(
                query,
                new RetrievalPlan(true, true, true, 8)
        );

        assertThat(ports.rewriteInput).isEqualTo("退款多久到账？");
        assertThat(ports.embeddingInput).isEqualTo("退款到账时效");
        assertThat(ports.lexicalInput).isEqualTo("退款到账时效");
        assertThat(ports.vectorTopK).isEqualTo(8);
        assertThat(ports.lexicalTopK).isEqualTo(8);
        assertThat(ports.rerankQuestion).isEqualTo("退款多久到账？");
        assertThat(ports.rerankCandidateIds).containsExactly("shared", "lexical", "semantic");
        assertThat(ports.rerankTopK).isEqualTo(3);
        assertThat(result.embeddingModel()).isEqualTo("embedding-v1");
        assertThat(result.chunks()).extracting(RetrievedKnowledgeChunk::chunkId)
                .containsExactly("shared", "semantic", "lexical");
    }

    @Test
    void vector_only_plan_does_not_call_rewrite_lexical_or_rerank_ports() {
        RecordingPorts ports = new RecordingPorts();

        KnowledgeRetrievalResult result = ports.service().retrieve(
                query(2),
                new RetrievalPlan(false, false, false, 4)
        );

        assertThat(ports.rewriteInput).isNull();
        assertThat(ports.lexicalInput).isNull();
        assertThat(ports.rerankQuestion).isNull();
        assertThat(result.chunks()).extracting(RetrievedKnowledgeChunk::chunkId)
                .containsExactly("semantic", "shared");
    }

    @Test
    void rejects_a_reranker_result_that_was_not_in_the_authorized_candidate_set() {
        RecordingPorts ports = new RecordingPorts();
        ports.rerankedIds = List.of("unknown-chunk");

        assertThatThrownBy(() -> ports.service().retrieve(
                query(2), new RetrievalPlan(false, true, true, 4)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown-chunk");
    }

    @Test
    void rejectsAnUnderfilledRerankerResultInsteadOfSilentlyDroppingCandidates() {
        RecordingPorts ports = new RecordingPorts();
        ports.rerankedIds = List.of("shared");

        assertThatThrownBy(() -> ports.service().retrieve(
                query(2), new RetrievalPlan(false, true, true, 4)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly 2");
    }

    private static RetrieveKnowledgeQuery query(int topK) {
        return new RetrieveKnowledgeQuery(
                "退款多久到账？",
                new KnowledgeAccessScope(new TenantId("tenant-a"), "user-1", List.of("support")),
                Instant.parse("2026-07-13T02:00:00Z"),
                topK
        );
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

    private static final class RecordingPorts implements
            KnowledgeQueryRewriter,
            KnowledgeEmbeddingModel,
            KnowledgeChunkSearchRepository,
            KnowledgeLexicalSearchRepository,
            KnowledgeReranker {

        private String rewriteInput;
        private String embeddingInput;
        private String lexicalInput;
        private int vectorTopK;
        private int lexicalTopK;
        private String rerankQuestion;
        private List<String> rerankCandidateIds;
        private int rerankTopK;
        private List<String> rerankedIds;

        private HybridKnowledgeRetrievalService service() {
            return new HybridKnowledgeRetrievalService(
                    this, this, this, this, this,
                    query -> new RetrievalPlan(false, false, false, 4),
                    new ReciprocalRankFusion(60)
            );
        }

        @Override
        public String rewrite(String question) {
            rewriteInput = question;
            return "退款到账时效";
        }

        @Override
        public KnowledgeEmbedding embed(String text) {
            embeddingInput = text;
            return new KnowledgeEmbedding(new float[]{0.1f, 0.2f}, "embedding-v1");
        }

        @Override
        public List<RetrievedKnowledgeChunk> search(
                float[] embedding,
                String embeddingModel,
                KnowledgeAccessScope scope,
                Instant effectiveAt,
                int topK
        ) {
            vectorTopK = topK;
            return List.of(chunk("semantic", 0.94d), chunk("shared", 0.88d));
        }

        @Override
        public List<RetrievedKnowledgeChunk> search(
                String query,
                KnowledgeAccessScope scope,
                Instant effectiveAt,
                int topK
        ) {
            lexicalInput = query;
            lexicalTopK = topK;
            return List.of(chunk("lexical", 21.0d), chunk("shared", 18.0d));
        }

        @Override
        public List<String> rerank(
                String question,
                List<RetrievedKnowledgeChunk> candidates,
                int topK
        ) {
            rerankQuestion = question;
            rerankCandidateIds = candidates.stream().map(RetrievedKnowledgeChunk::chunkId).toList();
            rerankTopK = topK;
            return rerankedIds == null
                    ? List.of(
                            candidates.getFirst().chunkId(),
                            candidates.get(2).chunkId(),
                            candidates.get(1).chunkId()
                    )
                    : rerankedIds;
        }
    }
}
