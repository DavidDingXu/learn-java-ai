package com.xiaoding.javaai.labs.alibaba;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

public final class OnlineRetrievalReplacementExperiment {

    private final TextEmbeddingGateway embeddingGateway;
    private final TextRerankGateway rerankGateway;
    private final LongSupplier nanoTime;

    public OnlineRetrievalReplacementExperiment(
            TextEmbeddingGateway embeddingGateway,
            TextRerankGateway rerankGateway) {
        this(embeddingGateway, rerankGateway, System::nanoTime);
    }

    OnlineRetrievalReplacementExperiment(
            TextEmbeddingGateway embeddingGateway,
            TextRerankGateway rerankGateway,
            LongSupplier nanoTime) {
        this.embeddingGateway = embeddingGateway;
        this.rerankGateway = rerankGateway;
        this.nanoTime = nanoTime;
    }

    public OnlineRetrievalExperimentReport run(
            String caseId,
            String query,
            List<RetrievalCandidate> candidates,
            List<String> expectedIds) {
        requireText(caseId, "caseId");
        requireText(query, "query");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }
        if (expectedIds == null || expectedIds.isEmpty()) {
            throw new IllegalArgumentException("expectedIds must not be empty");
        }
        Map<String, RetrievalCandidate> candidatesById = candidates.stream().collect(Collectors.toMap(
                RetrievalCandidate::id,
                candidate -> candidate,
                (left, right) -> {
                    throw new IllegalArgumentException("candidate ids must be unique");
                }));
        if (!candidatesById.keySet().containsAll(expectedIds)) {
            throw new IllegalArgumentException("expected ids must belong to the candidate set");
        }

        List<String> inputs = new ArrayList<>(candidates.size() + 1);
        inputs.add(query);
        candidates.stream().map(RetrievalCandidate::text).forEach(inputs::add);
        long embeddingStarted = nanoTime.getAsLong();
        List<float[]> vectors = embeddingGateway.embed(List.copyOf(inputs));
        Duration embeddingLatency = Duration.ofNanos(nanoTime.getAsLong() - embeddingStarted);
        validateVectors(vectors, inputs.size());

        float[] queryVector = vectors.getFirst();
        List<ScoredRetrievalCandidate> embeddingRanking = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            embeddingRanking.add(new ScoredRetrievalCandidate(
                    candidates.get(index).id(),
                    cosine(queryVector, vectors.get(index + 1))));
        }
        embeddingRanking.sort(Comparator.comparingDouble(ScoredRetrievalCandidate::score)
                .reversed()
                .thenComparing(ScoredRetrievalCandidate::id));
        List<RetrievalCandidate> rerankInput = embeddingRanking.stream()
                .map(candidate -> candidatesById.get(candidate.id()))
                .toList();

        long rerankStarted = nanoTime.getAsLong();
        List<ScoredRetrievalCandidate> reranked = List.copyOf(rerankGateway.rerank(query, rerankInput));
        Duration rerankLatency = Duration.ofNanos(nanoTime.getAsLong() - rerankStarted);
        requireSameCandidateIds(candidatesById.keySet(), reranked);
        List<String> rerankedIds = reranked.stream().map(ScoredRetrievalCandidate::id).toList();
        Set<String> expected = Set.copyOf(expectedIds);
        long matched = rerankedIds.stream().distinct().filter(expected::contains).count();
        double reciprocalRank = 0;
        for (int index = 0; index < rerankedIds.size(); index++) {
            if (expected.contains(rerankedIds.get(index))) {
                reciprocalRank = 1.0 / (index + 1);
                break;
            }
        }
        return new OnlineRetrievalExperimentReport(
                caseId,
                embeddingRanking.stream().map(ScoredRetrievalCandidate::id).toList(),
                rerankedIds,
                (double) matched / expected.size(),
                reciprocalRank,
                embeddingLatency,
                rerankLatency);
    }

    private static void validateVectors(List<float[]> vectors, int expectedSize) {
        if (vectors == null || vectors.size() != expectedSize) {
            throw new IllegalArgumentException("embedding provider must return one vector per input");
        }
        int dimensions = vectors.getFirst() == null ? 0 : vectors.getFirst().length;
        if (dimensions == 0 || vectors.stream().anyMatch(vector -> vector == null || vector.length != dimensions)) {
            throw new IllegalArgumentException("embedding vectors must have the same non-zero dimensions");
        }
    }

    private static void requireSameCandidateIds(
            Set<String> expectedIds,
            List<ScoredRetrievalCandidate> reranked) {
        Set<String> actualIds = reranked.stream()
                .map(ScoredRetrievalCandidate::id)
                .collect(Collectors.toCollection(HashSet::new));
        if (actualIds.size() != reranked.size() || !actualIds.equals(expectedIds)) {
            throw new IllegalArgumentException("reranker must return the same candidate ids exactly once");
        }
    }

    private static double cosine(float[] left, float[] right) {
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0 || rightNorm == 0) return 0;
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
