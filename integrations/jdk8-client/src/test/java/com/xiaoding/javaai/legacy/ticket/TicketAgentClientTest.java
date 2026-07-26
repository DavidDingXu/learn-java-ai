package com.xiaoding.javaai.legacy.ticket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketAgentClientTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void reads_a_versioned_confirmation_with_an_employee_bearer_token() throws Exception {
        final AtomicReference<String> authorization = new AtomicReference<String>();
        server.createContext("/api/v1/agent/tasks/task-100", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200,
                    "{\"taskId\":\"task-100\",\"caseId\":\"ticket-900\",\"status\":\"WAITING_CONFIRMATION\","
                            + "\"version\":2,\"outcome\":null,"
                            + "\"confirmation\":{\"confirmationId\":\"confirmation-100\",\"actionId\":\"action-100\","
                            + "\"toolName\":\"ASSIGN_QUEUE\",\"risk\":\"MEDIUM\",\"requiredRole\":\"TICKET_OPERATOR\","
                            + "\"arguments\":{\"queueCode\":\"refund-review\"},\"boundTaskVersion\":2,"
                            + "\"expiresAt\":\"2026-07-13T08:20:00Z\"},\"updatedAt\":\"2026-07-13T08:02:00Z\"}");
        });

        try (TicketAgentClient client = client(1000)) {
            AgentTaskView task = client.getTask("task-100");

            assertEquals("Bearer employee-token", authorization.get());
            assertEquals("WAITING_CONFIRMATION", task.getStatus());
            assertEquals(2L, task.getVersion());
            assertEquals("ASSIGN_QUEUE", task.getConfirmation().getToolName());
            assertEquals("refund-review", task.getConfirmation().getArguments().get("queueCode"));
        }
    }

    @Test
    void submits_confirmation_without_identity_fields_and_preserves_the_idempotency_key() throws Exception {
        final AtomicReference<String> idempotencyKey = new AtomicReference<String>();
        final AtomicReference<String> requestBody = new AtomicReference<String>();
        server.createContext("/api/v1/agent/tasks/task-100/confirmation", exchange -> {
            idempotencyKey.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            requestBody.set(readBody(exchange));
            respond(exchange, 200,
                    "{\"taskId\":\"task-100\",\"state\":\"COMPLETED\",\"actionId\":\"action-100\","
                            + "\"toolStatus\":\"SUCCEEDED\",\"auditId\":\"legacy-audit-100\","
                            + "\"taskVersion\":4,\"duplicate\":false}");
        });

        try (TicketAgentClient client = client(1000)) {
            ConfirmationDecisionReceipt receipt = client.confirm(
                    "task-100",
                    "confirm:task-100:decision-1",
                    new ConfirmToolActionRequest(
                            "confirmation-100", 2L, "APPROVE", "已核对工单"));

            assertEquals("confirm:task-100:decision-1", idempotencyKey.get());
            assertTrue(requestBody.get().contains("\"expectedTaskVersion\":2"));
            assertFalse(requestBody.get().contains("tenantId"));
            assertFalse(requestBody.get().contains("roles"));
            assertEquals("COMPLETED", receipt.getState());
            assertEquals("legacy-audit-100", receipt.getAuditId());
        }
    }

    @Test
    void maps_a_business_conflict_without_marking_it_retryable() throws Exception {
        server.createContext("/api/v1/agent/tasks/task-100/confirmation", exchange -> respond(
                exchange, 409,
                "{\"code\":\"CONFIRMATION_IDEMPOTENCY_CONFLICT\",\"message\":\"key conflict\"}"));

        try (TicketAgentClient client = client(1000)) {
            TicketAgentClientException error = assertThrows(
                    TicketAgentClientException.class,
                    () -> client.confirm(
                            "task-100",
                            "confirm:task-100:decision-1",
                            new ConfirmToolActionRequest(
                                    "confirmation-100", 2L, "APPROVE", "approve")));

            assertEquals(409, error.getStatusCode());
            assertEquals("CONFIRMATION_IDEMPOTENCY_CONFLICT", error.getCode());
            assertFalse(error.isRetryable());
        }
    }

    @Test
    void reports_unknown_confirmation_outcome_after_a_timeout_and_never_retries_automatically() {
        final AtomicInteger requests = new AtomicInteger();
        server.createContext("/api/v1/agent/tasks/task-100/confirmation", exchange -> {
            requests.incrementAndGet();
            try {
                Thread.sleep(300L);
                respond(exchange, 200, "{}");
            } catch (Exception ignored) {
                exchange.close();
            }
        });

        try (TicketAgentClient client = client(100)) {
            ConfirmationOutcomeUnknownException error = assertThrows(
                    ConfirmationOutcomeUnknownException.class,
                    () -> client.confirm(
                            "task-100",
                            "confirm:task-100:decision-1",
                            new ConfirmToolActionRequest(
                                    "confirmation-100", 2L, "APPROVE", "approve")));

            assertEquals("confirm:task-100:decision-1", error.getIdempotencyKey());
            assertEquals(1, requests.get());
        }
    }

    @Test
    void treats_local_request_serialization_failure_as_known_before_transport() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("serialization failed") { };
            }
        };

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> TicketAgentClient.serializeConfirmationRequest(
                        failingMapper,
                        new ConfirmToolActionRequest(
                                "confirmation-100", 2L, "APPROVE", "approve")));

        assertTrue(error.getMessage().contains("serialize confirmation request"));
    }

    private TicketAgentClient client(int responseTimeoutMillis) {
        return TicketAgentClient.create(
                new TicketAgentClientConfig(baseUrl, 500, responseTimeoutMillis, 4, 20),
                () -> "employee-token");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        InputStream input = exchange.getRequestBody();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = input.read(buffer)) >= 0) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().put(
                "Content-Type", Collections.singletonList("application/json"));
        exchange.sendResponseHeaders(status, bytes.length);
        OutputStream output = exchange.getResponseBody();
        output.write(bytes);
        output.close();
    }
}
