package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.agent.application.RemoteExecutionUncertainException;
import com.xiaoding.javaai.ticket.agent.application.ToolExecutionRejectedException;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.agent.domain.ToolRisk;
import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.task.AgentTask;
import com.xiaoding.javaai.ticket.task.AgentTaskRequest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpLegacyWriteToolExecutorTest {

    private static final String TOOL_IDEMPOTENCY_KEY = "tool:v1:" + "a".repeat(64);

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    void sends_only_server_bound_action_fields_and_maps_the_idempotent_receipt() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"actionId":"action-100","status":"SUCCEEDED","duplicate":false,"auditId":"legacy-audit-100"}
                        """));
        DownstreamAccessTokenProvider tokens = (task, audience, scope) -> {
            assertThat(audience).isEqualTo("legacy-tool-service");
            assertThat(scope).isEqualTo("legacy:tool:execute");
            return "legacy-service-token";
        };
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(), tokens, Duration.ofSeconds(2));

        ToolExecutionReceipt receipt = executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY);

        assertThat(receipt.status()).isEqualTo("SUCCEEDED");
        assertThat(receipt.auditId()).isEqualTo("legacy-audit-100");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/tool-actions");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer legacy-service-token");
        assertThat(request.getHeader("Idempotency-Key")).isEqualTo(TOOL_IDEMPOTENCY_KEY);
        assertThat(request.getBody().readUtf8())
                .contains("action-100", "ticket-900", "ASSIGN_QUEUE", "refund-review")
                .doesNotContain("tenant-a", "customer-42", "TICKET_OPERATOR");
    }

    @Test
    void treats_a_timeout_after_send_as_an_unknown_outcome_instead_of_retrying() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofMillis(100));

        assertThatThrownBy(() -> executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY))
                .isInstanceOf(RemoteExecutionUncertainException.class);

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void maps_a_client_error_to_a_deterministic_tool_rejection() {
        server.enqueue(new MockResponse()
                .setResponseCode(422)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"code":"QUEUE_CLOSED","message":"target queue is closed"}
                        """));
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofSeconds(2));

        assertThatThrownBy(() -> executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY))
                .isInstanceOf(ToolExecutionRejectedException.class)
                .hasMessageContaining("QUEUE_CLOSED")
                .hasMessageContaining("target queue is closed");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void maps_a_server_error_to_an_unknown_outcome_instead_of_retrying() {
        server.enqueue(new MockResponse().setResponseCode(503));
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofSeconds(2));

        assertThatThrownBy(() -> executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY))
                .isInstanceOf(RemoteExecutionUncertainException.class)
                .hasMessageContaining("503");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void treats_an_invalid_success_response_as_unknown_because_the_action_may_have_run() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("not-json"));
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofSeconds(2));

        assertThatThrownBy(() -> executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY))
                .isInstanceOf(RemoteExecutionUncertainException.class)
                .hasMessageContaining("response");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void treats_an_unknown_success_status_as_unknown_instead_of_completing_the_task() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"actionId":"action-100","status":"PENDING","duplicate":false,"auditId":"legacy-audit-100"}
                        """));
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofSeconds(2));

        assertThatThrownBy(() -> executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY))
                .isInstanceOf(RemoteExecutionUncertainException.class)
                .hasMessageContaining("response");
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void treats_request_serialization_failure_as_a_local_error_before_send() {
        ObjectMapper failingMapper = new ObjectMapper() {
            @Override
            public String writeValueAsString(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("serialization failed") { };
            }
        };
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofSeconds(2), failingMapper);

        assertThatThrownBy(() -> executor.execute(
                task(), confirmation(), TOOL_IDEMPOTENCY_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("serialize legacy tool request");
        assertThat(server.getRequestCount()).isZero();
    }

    @Test
    void rejects_a_blank_idempotency_key_before_send() {
        HttpLegacyWriteToolExecutor executor = new HttpLegacyWriteToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "legacy-service-token",
                Duration.ofSeconds(2));

        assertThatThrownBy(() -> executor.execute(task(), confirmation(), " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idempotencyKey");
        assertThat(server.getRequestCount()).isZero();
    }

    private static ConfirmationRequest confirmation() {
        return new ConfirmationRequest(
                "confirmation-100", "action-100", "ASSIGN_QUEUE",
                ToolRisk.MEDIUM, "TICKET_OPERATOR",
                Map.of("queueCode", "refund-review"), "fingerprint", 2,
                Instant.parse("2026-07-13T08:20:00Z"));
    }

    private static AgentTask task() {
        return AgentTask.accepted(
                "task-100",
                new DelegatedTicketIdentity(
                        "tenant-a", "customer-42", "customer-bff",
                        List.of("CUSTOMER"), List.of("retail")),
                new AgentTaskRequest("ticket-900", "resolve", Map.of()),
                Instant.parse("2026-07-13T08:00:00Z"));
    }
}
