package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;

import java.text.Normalizer;
import java.util.Locale;

public final class DeterministicHashEmbeddingModel implements KnowledgeEmbeddingModel {

    private final int dimensions;

    public DeterministicHashEmbeddingModel(int dimensions) {
        if (dimensions < 8) throw new IllegalArgumentException("dimensions must be at least 8");
        this.dimensions = dimensions;
    }

    @Override
    public KnowledgeEmbedding embed(String text) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("text must not be blank");
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .strip();
        int[] codePoints = normalized.codePoints().toArray();
        float[] vector = new float[dimensions];
        for (int index = 0; index < codePoints.length; index += 1) {
            addFeature(vector, Integer.toString(codePoints[index]));
            if (index + 1 < codePoints.length) {
                addFeature(vector, codePoints[index] + ":" + codePoints[index + 1]);
            }
        }
        normalize(vector);
        return new KnowledgeEmbedding(vector, "deterministic-hash-v1-" + dimensions);
    }

    private void addFeature(float[] vector, String feature) {
        int hash = feature.hashCode();
        int position = Math.floorMod(hash, dimensions);
        vector[position] += (hash & 1) == 0 ? 1.0f : -1.0f;
    }

    private static void normalize(float[] vector) {
        double squared = 0;
        for (float value : vector) squared += value * value;
        if (squared == 0) throw new IllegalArgumentException("text produced an empty embedding");
        float norm = (float) Math.sqrt(squared);
        for (int index = 0; index < vector.length; index += 1) vector[index] /= norm;
    }
}
