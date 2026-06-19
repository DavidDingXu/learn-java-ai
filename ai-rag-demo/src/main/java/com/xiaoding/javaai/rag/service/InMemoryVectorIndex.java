package com.xiaoding.javaai.rag.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryVectorIndex implements VectorIndex {

    private final Map<String, VectorIndexEntry> entries = new LinkedHashMap<>();
    private final DocumentAccessFilter accessFilter;

    public InMemoryVectorIndex() {
        this(new DocumentAccessFilter());
    }

    @Autowired
    public InMemoryVectorIndex(DocumentAccessFilter accessFilter) {
        this.accessFilter = accessFilter == null ? new DocumentAccessFilter() : accessFilter;
    }

    @Override
    public void upsert(List<VectorIndexEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (VectorIndexEntry entry : entries) {
            this.entries.put(entry.chunk().chunkId(), entry);
        }
    }

    @Override
    public List<VectorSearchResult> search(List<Double> queryVector, OperatorScope scope, int topK) {
        if (queryVector == null || queryVector.isEmpty() || topK <= 0) {
            return List.of();
        }
        return entries.values().stream()
                .filter(entry -> accessFilter.decide(entry.chunk(), scope).allowed())
                .map(entry -> new VectorSearchResult(entry.chunk(), cosine(queryVector, entry.vector())))
                .filter(result -> result.score() > 0)
                .sorted(Comparator.comparing(VectorSearchResult::score).reversed()
                        .thenComparing(result -> result.chunk().documentId())
                        .thenComparing(result -> result.chunk().chunkId()))
                .limit(topK)
                .toList();
    }

    private double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        if (size == 0) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < size; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
