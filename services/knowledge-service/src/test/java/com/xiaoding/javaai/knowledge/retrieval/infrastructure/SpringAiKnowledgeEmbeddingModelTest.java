package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiKnowledgeEmbeddingModelTest {

    @Test
    void maps_the_vector_and_provider_model_without_leaking_spring_ai_types() {
        var adapter = new SpringAiKnowledgeEmbeddingModel(new StubEmbeddingModel(), 3);

        var embedding = adapter.embed("退款多久到账？");

        assertThat(embedding.vector()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(embedding.model()).isEqualTo("text-embedding-v3");
    }

    @Test
    void mapsABatchByProviderIndexAndKeepsOneModelContract() {
        var adapter = new SpringAiKnowledgeEmbeddingModel(new StubEmbeddingModel(), 3);

        List<KnowledgeEmbedding> embeddings = adapter.embedAll(List.of("第一段", "第二段"));

        assertThat(embeddings).extracting(KnowledgeEmbedding::vector)
                .containsExactly(
                        new float[]{0.1f, 0.2f, 0.3f},
                        new float[]{0.4f, 0.5f, 0.6f}
                );
        assertThat(embeddings).extracting(KnowledgeEmbedding::model)
                .containsOnly("text-embedding-v3");
    }

    private static final class StubEmbeddingModel implements EmbeddingModel {

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            if (request.getInstructions().size() == 2) {
                return new EmbeddingResponse(
                        List.of(
                                new Embedding(new float[]{0.4f, 0.5f, 0.6f}, 1),
                                new Embedding(new float[]{0.1f, 0.2f, 0.3f}, 0)
                        ),
                        new EmbeddingResponseMetadata("text-embedding-v3", null)
                );
            }
            return new EmbeddingResponse(
                    List.of(new Embedding(new float[]{0.1f, 0.2f, 0.3f}, 0)),
                    new EmbeddingResponseMetadata("text-embedding-v3", null)
            );
        }

        @Override
        public float[] embed(Document document) {
            return new float[]{0.1f, 0.2f, 0.3f};
        }
    }
}
