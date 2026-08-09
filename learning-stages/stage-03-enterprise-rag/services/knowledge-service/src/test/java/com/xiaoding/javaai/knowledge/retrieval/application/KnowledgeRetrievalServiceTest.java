package com.xiaoding.javaai.knowledge.retrieval.application;

import com.xiaoding.javaai.knowledge.document.domain.DocumentId;
import com.xiaoding.javaai.knowledge.document.domain.TenantId;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeChunkSearchRepository;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeRetrievalServiceTest {

    @Test
    void embeds_the_question_and_searches_with_the_trusted_access_scope() {
        RecordingEmbeddingModel embeddingModel = new RecordingEmbeddingModel();
        RecordingSearchRepository repository = new RecordingSearchRepository();
        KnowledgeRetrievalService service = new KnowledgeRetrievalService(embeddingModel, repository);
        KnowledgeAccessScope scope = new KnowledgeAccessScope(
                new TenantId("tenant-a"), "user-1", List.of("support")
        );
        Instant now = Instant.parse("2026-07-13T01:00:00Z");

        KnowledgeRetrievalResult result = service.retrieve(new RetrieveKnowledgeQuery(
                "退款多久能到账？", scope, now, 5
        ));

        assertThat(embeddingModel.input).isEqualTo("退款多久能到账？");
        assertThat(repository.embedding).containsExactly(0.1f, 0.2f);
        assertThat(repository.embeddingModel).isEqualTo("test-embedding");
        assertThat(repository.scope).isSameAs(scope);
        assertThat(repository.effectiveAt).isEqualTo(now);
        assertThat(repository.topK).isEqualTo(5);
        assertThat(result.embeddingModel()).isEqualTo("test-embedding");
        assertThat(result.chunks()).extracting(RetrievedKnowledgeChunk::chunkId)
                .containsExactly("chunk-1");
    }

    private static final class RecordingEmbeddingModel implements KnowledgeEmbeddingModel {
        private String input;

        @Override
        public KnowledgeEmbedding embed(String text) {
            input = text;
            return new KnowledgeEmbedding(new float[]{0.1f, 0.2f}, "test-embedding");
        }
    }

    private static final class RecordingSearchRepository implements KnowledgeChunkSearchRepository {
        private float[] embedding;
        private String embeddingModel;
        private KnowledgeAccessScope scope;
        private Instant effectiveAt;
        private int topK;

        @Override
        public List<RetrievedKnowledgeChunk> search(
                float[] embedding,
                String embeddingModel,
                KnowledgeAccessScope scope,
                Instant effectiveAt,
                int topK
        ) {
            this.embedding = embedding;
            this.embeddingModel = embeddingModel;
            this.scope = scope;
            this.effectiveAt = effectiveAt;
            this.topK = topK;
            return List.of(new RetrievedKnowledgeChunk(
                    "chunk-1",
                    new DocumentId("refund-policy"),
                    2,
                    List.of("退款", "到账时间"),
                    "第十条",
                    "一到五个工作日",
                    0.93d
            ));
        }
    }
}
