package com.xiaoding.javaai.eval.agent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentTaskHttpEvaluationClientTest {

    private HttpServer server;
    private final List<String> calls = new ArrayList<>();

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/agent/tasks", this::handle);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void uses_separate_tokens_for_create_run_and_audit_without_confirming() {
        URI baseUrl = URI.create("http://127.0.0.1:" + server.getAddress().getPort());
        AgentEvaluationSnapshot snapshot = new AgentTaskHttpEvaluationClient().evaluate(
                baseUrl,
                new AgentEvaluationTokens("create-token", "run-token", "read-token"),
                new AgentEvalCase(
                        "assign", "assign ticket", Map.of("queueCode", "refund-review"),
                        "WAITING_CONFIRMATION", "ASSIGN_QUEUE", "MEDIUM", "TICKET_OPERATOR",
                        Map.of("queueCode", "refund-review"),
                        List.of("TOOL_EXECUTION_SUCCEEDED"), List.of("customerPhone")),
                "agent-eval:assign:commit");

        assertEquals("WAITING_CONFIRMATION", snapshot.state());
        assertEquals("ASSIGN_QUEUE", snapshot.toolName());
        assertEquals(Map.of("queueCode", "refund-review"), snapshot.arguments());
        assertEquals(List.of("action"), snapshot.auditDetails());
        assertEquals(List.of(
                "POST /api/v1/agent/tasks Bearer create-token",
                "POST /api/v1/agent/tasks/task-100/runs Bearer run-token",
                "GET /api/v1/agent/tasks/task-100/audit Bearer read-token"
        ), calls);
    }

    private void handle(HttpExchange exchange) throws java.io.IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        calls.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + " " + authorization);
        String path = exchange.getRequestURI().getPath();
        String body;
        int status;
        if ("POST".equals(exchange.getRequestMethod()) && "/api/v1/agent/tasks".equals(path)) {
            assertEquals("agent-eval:assign:commit", exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            status = 202;
            body = "{\"taskId\":\"task-100\",\"status\":\"ACCEPTED\",\"duplicate\":false}";
        } else if (path.endsWith("/runs")) {
            status = 200;
            body = """
                    {"taskId":"task-100","caseId":"assign","status":"WAITING_CONFIRMATION","version":2,
                     "confirmation":{"confirmationId":"confirm-1","actionId":"action-1","toolName":"ASSIGN_QUEUE","risk":"MEDIUM","requiredRole":"TICKET_OPERATOR","arguments":{"queueCode":"refund-review"},"boundTaskVersion":2,"expiresAt":"2026-07-13T08:15:00Z"},
                     "updatedAt":"2026-07-13T08:00:00Z"}
                    """;
        } else {
            status = 200;
            body = "[{\"sequence\":1,\"eventType\":\"CONFIRMATION_REQUESTED\",\"actorId\":\"worker\",\"detail\":\"action\",\"occurredAt\":\"2026-07-13T08:00:00Z\"}]";
        }
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }
}
