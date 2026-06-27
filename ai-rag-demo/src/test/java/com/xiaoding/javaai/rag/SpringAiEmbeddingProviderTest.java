package com.xiaoding.javaai.rag;

import com.xiaoding.javaai.rag.service.springai.SpringAiEmbeddingProvider;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiEmbeddingProviderTest {

    @Test
    void adaptsSpringAiEmbeddingModelWithoutChangingRagBoundary() {
        RecordingEmbeddingModel model = new RecordingEmbeddingModel();
        SpringAiEmbeddingProvider provider = new SpringAiEmbeddingProvider(model);

        List<List<Double>> vectors = provider.embed(List.of("发货后退款", "人工复核"));

        assertThat(model.inputs()).containsExactly("发货后退款", "人工复核");
        assertThat(vectors).containsExactly(
                List.of(5.0, 0.0),
                List.of(4.0, 1.0)
        );
    }

    private static final class RecordingEmbeddingModel implements EmbeddingModel {
        private List<String> inputs = List.of();

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            inputs = List.copyOf(request.getInstructions());
            List<Embedding> embeddings = inputs.stream()
                    .map(text -> new Embedding(new float[]{text.length(), inputs.indexOf(text)}, inputs.indexOf(text)))
                    .toList();
            return new EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(Document document) {
            return embed(document.getText());
        }

        private List<String> inputs() {
            return inputs;
        }
    }
}
