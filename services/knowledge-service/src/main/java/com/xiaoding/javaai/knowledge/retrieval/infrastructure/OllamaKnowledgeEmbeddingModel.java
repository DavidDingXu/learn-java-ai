package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.knowledge.retrieval.application.KnowledgeEmbedding;
import com.xiaoding.javaai.knowledge.retrieval.application.port.KnowledgeEmbeddingModel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class OllamaKnowledgeEmbeddingModel implements KnowledgeEmbeddingModel {

    private final URI endpoint;
    private final String model;
    private final int expectedDimensions;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration requestTimeout;

    public OllamaKnowledgeEmbeddingModel(
            String baseUrl,
            String model,
            int expectedDimensions,
            ObjectMapper objectMapper
    ) {
        this(baseUrl, model, expectedDimensions, objectMapper, Duration.ofMinutes(2));
    }

    public OllamaKnowledgeEmbeddingModel(
            String baseUrl,
            String model,
            int expectedDimensions,
            ObjectMapper objectMapper,
            Duration requestTimeout
    ) {
        if (baseUrl == null || baseUrl.isBlank()) throw new IllegalArgumentException("baseUrl must not be blank");
        if (model == null || model.isBlank()) throw new IllegalArgumentException("model must not be blank");
        if (expectedDimensions < 1) throw new IllegalArgumentException("expectedDimensions must be positive");
        if (objectMapper == null) throw new IllegalArgumentException("objectMapper must not be null");
        if (requestTimeout == null || requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        this.endpoint = URI.create(baseUrl.replaceAll("/+$", "") + "/api/embed");
        this.model = model.strip();
        this.expectedDimensions = expectedDimensions;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.requestTimeout = requestTimeout;
    }

    @Override
    public KnowledgeEmbedding embed(String text) {
        return embedAll(List.of(text)).getFirst();
    }

    @Override
    public List<KnowledgeEmbedding> embedAll(List<String> texts) {
        validateTexts(texts);
        try {
            String body = objectMapper.writeValueAsString(
                    new OllamaEmbedRequest(model, List.copyOf(texts), true, expectedDimensions)
            );
            var request = HttpRequest.newBuilder(endpoint)
                    .header("Content-Type", "application/json")
                    .timeout(requestTimeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Ollama embedding request failed with HTTP " + response.statusCode());
            }
            return toEmbeddings(objectMapper.readValue(response.body(), OllamaEmbedResponse.class), texts.size());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ollama embedding request was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Ollama embedding request failed", exception);
        }
    }

    private List<KnowledgeEmbedding> toEmbeddings(OllamaEmbedResponse response, int expectedCount) {
        if (response.model() == null || response.model().isBlank()) {
            throw new IllegalStateException("Ollama returned no embedding model");
        }
        if (response.embeddings() == null || response.embeddings().size() != expectedCount) {
            throw new IllegalStateException(
                    "Ollama returned " + (response.embeddings() == null ? 0 : response.embeddings().size())
                            + " vectors for " + expectedCount + " texts"
            );
        }
        var result = new ArrayList<KnowledgeEmbedding>(expectedCount);
        for (List<Float> values : response.embeddings()) {
            if (values == null || values.size() != expectedDimensions) {
                throw new IllegalStateException(
                        "Ollama embedding dimensions " + (values == null ? 0 : values.size())
                                + " do not match expected " + expectedDimensions
                );
            }
            float[] vector = new float[expectedDimensions];
            for (int index = 0; index < values.size(); index += 1) {
                Float value = values.get(index);
                if (value == null || !Float.isFinite(value)) {
                    throw new IllegalStateException("Ollama returned an invalid embedding value");
                }
                vector[index] = value;
            }
            result.add(new KnowledgeEmbedding(vector, response.model()));
        }
        return List.copyOf(result);
    }

    private static void validateTexts(List<String> texts) {
        if (texts == null || texts.isEmpty()) throw new IllegalArgumentException("texts must not be empty");
        if (texts.stream().anyMatch(text -> text == null || text.isBlank())) {
            throw new IllegalArgumentException("texts must not contain blank values");
        }
    }

    private record OllamaEmbedRequest(String model, List<String> input, boolean truncate, int dimensions) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OllamaEmbedResponse(String model, List<List<Float>> embeddings) { }
}
