package com.xiaoding.javaai.eval.model;

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

public final class KnowledgeAnswerHttpClient implements KnowledgeAnswerClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String bearerToken;

    public KnowledgeAnswerHttpClient() {
        this(null);
    }

    public KnowledgeAnswerHttpClient(String bearerToken) {
        this(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(),
                new ObjectMapper(),
                bearerToken
        );
    }

    KnowledgeAnswerHttpClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this(httpClient, objectMapper, null);
    }

    KnowledgeAnswerHttpClient(HttpClient httpClient, ObjectMapper objectMapper, String bearerToken) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.bearerToken = bearerToken == null || bearerToken.isBlank() ? null : bearerToken.strip();
    }

    @Override
    public KnowledgeAnswerSnapshot answer(URI baseUrl, String question) {
        try {
            HttpRequest request = buildRequest(baseUrl, question);
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new KnowledgeAnswerClientException(
                        "knowledge answer request returned HTTP " + response.statusCode()
                );
            }
            return parse(response.body());
        } catch (KnowledgeAnswerClientException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KnowledgeAnswerClientException("knowledge answer request was interrupted", exception);
        } catch (Exception exception) {
            throw new KnowledgeAnswerClientException("knowledge answer request failed", exception);
        }
    }

    HttpRequest buildRequest(URI baseUrl, String question) {
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("question", question));
            URI endpoint = URI.create(trimTrailingSlash(baseUrl.toString()) + "/api/v1/knowledge/answers");
            HttpRequest.Builder builder = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "application/json");
            if (bearerToken != null) builder.header("Authorization", "Bearer " + bearerToken);
            return builder.POST(HttpRequest.BodyPublishers.ofString(requestBody)).build();
        } catch (Exception exception) {
            throw new KnowledgeAnswerClientException("knowledge answer request could not be created", exception);
        }
    }

    private KnowledgeAnswerSnapshot parse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        List<String> citations = new ArrayList<>();
        root.path("citations").forEach(node -> citations.add(node.path("sectionId").asText()));
        JsonNode usage = root.path("usage");
        return new KnowledgeAnswerSnapshot(
                requiredText(root, "answer"),
                citations,
                root.path("refused").asBoolean(false),
                root.path("refusalReason").asText(""),
                requiredText(root, "model"),
                requiredText(root, "traceId"),
                usage.path("promptTokens").asInt(-1),
                usage.path("completionTokens").asInt(-1),
                usage.path("totalTokens").asInt(-1)
        );
    }

    private static String requiredText(JsonNode root, String field) {
        String value = root.path(field).asText();
        if (value.isBlank()) throw new KnowledgeAnswerClientException("response field is missing: " + field);
        return value;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
