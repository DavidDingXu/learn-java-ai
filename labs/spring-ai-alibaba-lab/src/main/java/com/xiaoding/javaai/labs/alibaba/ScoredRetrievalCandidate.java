package com.xiaoding.javaai.labs.alibaba;

public record ScoredRetrievalCandidate(String id, double score) {

    public ScoredRetrievalCandidate {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("score must be finite");
        }
        id = id.strip();
    }
}
