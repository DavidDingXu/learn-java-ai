package com.xiaoding.javaai.rag.service.springai;

import com.xiaoding.javaai.rag.service.EmbeddingProvider;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

public class SpringAiEmbeddingProvider implements EmbeddingProvider {

    private final EmbeddingModel embeddingModel;

    public SpringAiEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        return embeddingModel.embed(texts).stream()
                .map(SpringAiEmbeddingProvider::toDoubleList)
                .toList();
    }

    private static List<Double> toDoubleList(float[] vector) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException("Spring AI EmbeddingModel returned an empty vector");
        }
        return java.util.stream.IntStream.range(0, vector.length)
                .mapToObj(index -> (double) vector[index])
                .toList();
    }
}
