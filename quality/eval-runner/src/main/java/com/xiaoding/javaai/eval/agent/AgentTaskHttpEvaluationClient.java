package com.xiaoding.javaai.eval.agent;

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
import java.util.regex.Pattern;

public final class AgentTaskHttpEvaluationClient implements AgentEvaluationClient {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public AgentTaskHttpEvaluationClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build(), new ObjectMapper());
    }

    AgentTaskHttpEvaluationClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AgentEvaluationSnapshot evaluate(
            URI baseUrl,
            AgentEvaluationTokens tokens,
            AgentEvalCase evalCase,
            String idempotencyKey
    ) {
        long startedAt = System.nanoTime();
        String root = trimTrailingSlash(baseUrl.toString());
        try {
            String taskId = createTask(root, tokens.createToken(), evalCase, idempotencyKey);
            JsonNode task = sendJson(HttpRequest.newBuilder(URI.create(
                            root + "/api/v1/agent/tasks/" + requireSafeId(taskId) + "/runs"))
                    .timeout(Duration.ofSeconds(90))
                    .header("Authorization", "Bearer " + tokens.runToken())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(), "run agent task");
            JsonNode audit = sendJson(HttpRequest.newBuilder(URI.create(
                            root + "/api/v1/agent/tasks/" + requireSafeId(taskId) + "/audit"))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + tokens.readToken())
                    .GET()
                    .build(), "read agent audit");
            List<String> eventTypes = new ArrayList<>();
            List<String> eventDetails = new ArrayList<>();
            audit.forEach(event -> {
                eventTypes.add(event.path("eventType").asText());
                eventDetails.add(event.path("detail").asText());
            });
            JsonNode confirmation = task.path("confirmation");
            return new AgentEvaluationSnapshot(
                    taskId,
                    requiredField(task, "status"),
                    nullableField(confirmation, "toolName"),
                    nullableField(confirmation, "risk"),
                    nullableField(confirmation, "requiredRole"),
                    eventTypes,
                    eventDetails,
                    Duration.ofNanos(System.nanoTime() - startedAt).toMillis());
        } catch (AgentEvaluationClientException error) {
            throw error;
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new AgentEvaluationClientException("agent evaluation request was interrupted", error);
        } catch (Exception error) {
            throw new AgentEvaluationClientException("agent evaluation request failed", error);
        }
    }

    private String createTask(
            String root,
            String token,
            AgentEvalCase evalCase,
            String idempotencyKey
    ) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "caseId", evalCase.id(),
                "objective", evalCase.objective(),
                "businessContext", evalCase.businessContext()));
        JsonNode receipt = sendJson(HttpRequest.newBuilder(URI.create(root + "/api/v1/agent/tasks"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", idempotencyKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), "create agent task");
        return requiredField(receipt, "taskId");
    }

    private JsonNode sendJson(HttpRequest request, String operation) throws Exception {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new AgentEvaluationClientException(
                    operation + " returned HTTP " + response.statusCode());
        }
        return objectMapper.readTree(response.body());
    }

    private static String requiredField(JsonNode node, String name) {
        String value = node.path(name).asText();
        if (value.isBlank()) throw new AgentEvaluationClientException("response field is missing: " + name);
        return value;
    }

    private static String nullableField(JsonNode node, String name) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        String value = node.path(name).asText();
        return value.isBlank() ? null : value;
    }

    private static String requireSafeId(String value) {
        if (!SAFE_ID.matcher(value).matches()) {
            throw new AgentEvaluationClientException("taskId contains unsupported characters");
        }
        return value;
    }

    private static String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
