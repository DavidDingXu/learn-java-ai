package com.xiaoding.javaai.knowledge.retrieval.application;

public record KnowledgeEmbedding(float[] vector, String model) {
    public KnowledgeEmbedding {
        if (vector == null || vector.length == 0) throw new IllegalArgumentException("vector must not be empty");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model must not be blank");
        for (float value : vector) {
            if (!Float.isFinite(value)) throw new IllegalArgumentException("vector must contain only finite values");
        }
        vector = vector.clone();
    }

    @Override
    public float[] vector() {
        return vector.clone();
    }
}
