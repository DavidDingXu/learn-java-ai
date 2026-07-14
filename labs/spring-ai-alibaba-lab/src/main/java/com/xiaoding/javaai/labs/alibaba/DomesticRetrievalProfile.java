package com.xiaoding.javaai.labs.alibaba;

public record DomesticRetrievalProfile(
        String embeddingModel,
        int dimensions,
        String rerankModel,
        int rerankTopN) {

    public DomesticRetrievalProfile {
        if (embeddingModel == null || embeddingModel.isBlank()) {
            throw new IllegalArgumentException("embeddingModel must not be blank");
        }
        if (dimensions <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
        if (rerankModel == null || rerankModel.isBlank()) {
            throw new IllegalArgumentException("rerankModel must not be blank");
        }
        if (rerankTopN <= 0) {
            throw new IllegalArgumentException("rerankTopN must be positive");
        }
    }

    public boolean requiresFullReindexFrom(DomesticRetrievalProfile previous) {
        return previous == null
                || dimensions != previous.dimensions
                || !embeddingModel.equals(previous.embeddingModel);
    }
}
