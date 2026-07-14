package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Comparator;
import java.util.List;

public final class SpringAiKnowledgeEmbeddingModel implements KnowledgeEmbeddingModel {

    private final EmbeddingModel delegate;
    private final int expectedDimensions;

    public SpringAiKnowledgeEmbeddingModel(EmbeddingModel delegate, int expectedDimensions) {
        if (expectedDimensions < 1) throw new IllegalArgumentException("expectedDimensions must be positive");
        this.delegate = delegate;
        this.expectedDimensions = expectedDimensions;
    }

    @Override
    public KnowledgeEmbedding embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<KnowledgeEmbedding> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) throw new IllegalArgumentException("texts must not be empty");
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("texts must not contain blank values");
        }
        var response = delegate.embedForResponse(List.copyOf(texts));
        String model = response.getMetadata() == null ? null : response.getMetadata().getModel();
        if (model == null || model.isBlank()) {
            throw new IllegalStateException("embedding provider returned no model metadata");
        }
        var results = response.getResults();
        if (results == null || results.size() != texts.size()) {
            throw new IllegalStateException(
                    "embedding provider returned " + (results == null ? 0 : results.size())
                            + " vectors for " + texts.size() + " texts"
            );
        }
        var ordered = results.stream()
                .sorted(Comparator.comparing(result -> result.getIndex(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        var embeddings = new java.util.ArrayList<KnowledgeEmbedding>(ordered.size());
        for (int index = 0; index < ordered.size(); index += 1) {
            var result = ordered.get(index);
            if (result.getIndex() == null || result.getIndex() != index) {
                throw new IllegalStateException("embedding provider returned invalid result indexes");
            }
            embeddings.add(toKnowledgeEmbedding(result.getOutput(), model));
        }
        return List.copyOf(embeddings);
    }

    private KnowledgeEmbedding toKnowledgeEmbedding(float[] vector, String model) {
        if (vector == null) throw new IllegalStateException("embedding provider returned no vector");
        if (vector.length != expectedDimensions) {
            throw new IllegalStateException(
                    "embedding dimensions " + vector.length + " do not match expected " + expectedDimensions
            );
        }
        return new KnowledgeEmbedding(vector, model);
    }
}
