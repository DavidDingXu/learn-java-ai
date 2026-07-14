package com.xiaoding.javaai.eval.retrieval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KnowledgeRetrievalHttpClient implements RetrievalEvaluationClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public KnowledgeRetrievalHttpClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(), new ObjectMapper());
    }

    KnowledgeRetrievalHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public RetrievalClientResult retrieve(URI baseUrl, String bearerToken, String question, int topK) {
        long startedAt = System.nanoTime();
        try {
            HttpResponse<String> response = httpClient.send(
                    buildRequest(baseUrl, bearerToken, question, topK),
                    HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RetrievalEvaluationClientException(
                        "knowledge retrieval request returned HTTP " + response.statusCode()
                );
            }
            return parse(response.body(), elapsedMillis(startedAt));
        } catch (RetrievalEvaluationClientException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RetrievalEvaluationClientException("knowledge retrieval request was interrupted", exception);
        } catch (Exception exception) {
            throw new RetrievalEvaluationClientException("knowledge retrieval request failed", exception);
        }
    }

    HttpRequest buildRequest(URI baseUrl, String bearerToken, String question, int topK) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new IllegalArgumentException("bearerToken must not be blank");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of("question", question, "topK", topK));
            URI endpoint = URI.create(trimTrailingSlash(baseUrl.toString())
                    + "/internal/v1/knowledge/retrieval/evaluations");
            return HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken.strip())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
        } catch (Exception exception) {
            throw new RetrievalEvaluationClientException("knowledge retrieval request could not be created", exception);
        }
    }

    private RetrievalClientResult parse(String body, long latencyMillis) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        String embeddingModel = root.path("embeddingModel").asText();
        if (embeddingModel.isBlank()) {
            throw new RetrievalEvaluationClientException("response field is missing: embeddingModel");
        }
        List<String> chunkIds = new ArrayList<>();
        root.path("chunkIds").forEach(node -> chunkIds.add(node.asText()));
        return new RetrievalClientResult(embeddingModel, chunkIds, latencyMillis);
    }

    private static long elapsedMillis(long startedAt) {
        return Duration.ofNanos(System.nanoTime() - startedAt).toMillis();
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
