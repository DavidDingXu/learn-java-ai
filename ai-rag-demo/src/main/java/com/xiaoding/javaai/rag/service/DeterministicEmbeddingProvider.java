package com.xiaoding.javaai.rag.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicEmbeddingProvider implements EmbeddingProvider {

    @Override
    public List<List<Double>> embed(List<String> texts) {
        return texts.stream()
                .map(this::vector)
                .toList();
    }

    private List<Double> vector(String text) {
        double length = text.length();
        double hash = Math.abs(text.hashCode() % 1000);
        List<Double> vector = new ArrayList<>();
        vector.add(length / 100.0);
        vector.add(hash / 1000.0);
        vector.add((length + hash) / 1000.0);
        return vector;
    }
}
