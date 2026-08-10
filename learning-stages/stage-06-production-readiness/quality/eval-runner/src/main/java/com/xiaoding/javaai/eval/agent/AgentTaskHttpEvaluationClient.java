package com.xiaoding.javaai.eval.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
            HttpRequest.Builder runRequest = authorized(HttpRequest.newBuilder(URI.create(
                    root + "/api/v1/agent/tasks/" + requireSafeId(taskId) + "/runs")), tokens.runToken());
            JsonNode task = sendJson(runRequest.timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build(), "run agent task");
            HttpRequest.Builder auditRequest = authorized(HttpRequest.newBuilder(URI.create(
                    root + "/api/v1/agent/tasks/" + requireSafeId(taskId) + "/audit")), tokens.readToken());
            JsonNode audit = sendJson(auditRequest.timeout(Duration.ofSeconds(15))
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
                    stringMap(confirmation, "arguments"),
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
        HttpRequest.Builder createRequest = authorized(
                HttpRequest.newBuilder(URI.create(root + "/api/v1/agent/tasks")), token);
        JsonNode receipt = sendJson(createRequest.timeout(Duration.ofSeconds(15))
                .header("Idempotency-Key", idempotencyKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build(), "create agent task");
        return requiredField(receipt, "taskId");
    }

    private static HttpRequest.Builder authorized(HttpRequest.Builder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
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

    private static Map<String, String> stringMap(JsonNode node, String name) {
        if (node == null || node.isMissingNode() || node.isNull()) return Map.of();
        JsonNode value = node.path(name);
        if (value.isMissingNode() || value.isNull()) return Map.of();
        if (!value.isObject()) {
            throw new AgentEvaluationClientException("response field is not an object: " + name);
        }
        Map<String, String> result = new LinkedHashMap<>();
        value.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual()) {
                throw new AgentEvaluationClientException(
                        "response argument is not a string: " + entry.getKey());
            }
            result.put(entry.getKey(), entry.getValue().textValue());
        });
        return Map.copyOf(result);
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
