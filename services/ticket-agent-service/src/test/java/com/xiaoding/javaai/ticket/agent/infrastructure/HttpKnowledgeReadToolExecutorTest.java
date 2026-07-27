package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.agent.application.ReadToolUnavailableException;
import com.xiaoding.javaai.ticket.agent.domain.PreparedToolCall;
import com.xiaoding.javaai.ticket.agent.domain.ToolEffect;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpKnowledgeReadToolExecutorTest {

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
    void sends_a_delegated_query_and_returns_only_business_safe_observation_fields() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "answer":"退款通常需要 1 到 5 个工作日到账。",
                          "citations":[{"documentId":"refund-policy","version":"v1","sectionId":"arrival-time","title":"退款到账时间"}],
                          "refused":false,
                          "refusalReason":"",
                          "model":"provider-private-model",
                          "usage":{"promptTokens":30,"completionTokens":10,"totalTokens":40},
                          "finishReason":"stop",
                          "traceId":"trace-knowledge-1",
                          "executionMode":"LIVE_MODEL"
                        }
                        """));
        DownstreamAccessTokenProvider tokens = (task, audience, scope) -> {
            assertThat(task.identity().tenantId()).isEqualTo("tenant-a");
            assertThat(audience).isEqualTo("knowledge-service");
            assertThat(scope).isEqualTo("knowledge:answer");
            return "delegated-knowledge-token";
        };
        HttpKnowledgeReadToolExecutor executor = new HttpKnowledgeReadToolExecutor(
                RestClient.builder(), server.url("/").toString(), tokens,
                Duration.ofSeconds(2), fixedClock());

        ToolObservation observation = executor.execute(queryCall(), task());

        assertThat(observation.result())
                .containsEntry("answer", "退款通常需要 1 到 5 个工作日到账。")
                .containsEntry("citations", "refund-policy/v1#arrival-time")
                .containsEntry("traceId", "trace-knowledge-1")
                .doesNotContainKeys("model", "usage", "finishReason", "executionMode");
        RecordedRequest request = server.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/v1/knowledge/answers");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer delegated-knowledge-token");
        assertThat(request.getBody().readUtf8())
                .contains("退款多久到账？")
                .doesNotContain("tenant-a", "customer-42");
    }

    @Test
    void maps_a_client_rejection_without_exposing_the_downstream_body() {
        server.enqueue(new MockResponse()
                .setResponseCode(403)
                .setBody("customer-secret-should-not-leak"));

        assertThatThrownBy(() -> executor(Duration.ofSeconds(2)).execute(queryCall(), task()))
                .isInstanceOf(ReadToolUnavailableException.class)
                .satisfies(error -> assertThat(((ReadToolUnavailableException) error).failureKind())
                        .isEqualTo(ReadToolUnavailableException.FailureKind.REQUEST_REJECTED))
                .hasMessage("read tool dependency failed: REQUEST_REJECTED")
                .hasMessageNotContaining("customer-secret-should-not-leak");
    }

    @Test
    void maps_a_server_error_to_dependency_unavailable() {
        server.enqueue(new MockResponse().setResponseCode(503));

        assertThatThrownBy(() -> executor(Duration.ofSeconds(2)).execute(queryCall(), task()))
                .isInstanceOf(ReadToolUnavailableException.class)
                .satisfies(error -> assertThat(((ReadToolUnavailableException) error).failureKind())
                        .isEqualTo(ReadToolUnavailableException.FailureKind.DEPENDENCY_UNAVAILABLE));
    }

    @ParameterizedTest
    @ValueSource(ints = {408, 429})
    void maps_transient_client_statuses_to_dependency_unavailable(int status) {
        server.enqueue(new MockResponse().setResponseCode(status));

        assertThatThrownBy(() -> executor(Duration.ofSeconds(2)).execute(queryCall(), task()))
                .isInstanceOf(ReadToolUnavailableException.class)
                .satisfies(error -> assertThat(((ReadToolUnavailableException) error).failureKind())
                        .isEqualTo(ReadToolUnavailableException.FailureKind.DEPENDENCY_UNAVAILABLE));
    }

    @Test
    void maps_a_read_timeout_to_transport_failure_without_retrying() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        assertThatThrownBy(() -> executor(Duration.ofMillis(100)).execute(queryCall(), task()))
                .isInstanceOf(ReadToolUnavailableException.class)
                .satisfies(error -> assertThat(((ReadToolUnavailableException) error).failureKind())
                        .isEqualTo(ReadToolUnavailableException.FailureKind.TRANSPORT_FAILURE));
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void maps_invalid_json_and_missing_required_fields_to_invalid_response() {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("not-json"));
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("{\"answer\":\"answer\",\"refused\":false,\"citations\":[]}"));
        HttpKnowledgeReadToolExecutor executor = executor(Duration.ofSeconds(2));

        assertInvalidResponse(() -> executor.execute(queryCall(), task()));
        assertInvalidResponse(() -> executor.execute(queryCall(), task()));
    }

    private static void assertInvalidResponse(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(ReadToolUnavailableException.class)
                .satisfies(error -> assertThat(((ReadToolUnavailableException) error).failureKind())
                        .isEqualTo(ReadToolUnavailableException.FailureKind.INVALID_RESPONSE));
    }

    private HttpKnowledgeReadToolExecutor executor(Duration timeout) {
        return new HttpKnowledgeReadToolExecutor(
                RestClient.builder(), server.url("/").toString(),
                (task, audience, scope) -> "delegated-knowledge-token",
                timeout, fixedClock());
    }

    private static PreparedToolCall queryCall() {
        return new PreparedToolCall(
                "QUERY_KNOWLEDGE", ToolEffect.READ, ToolRisk.READ_ONLY, "",
                Map.of("question", "退款多久到账？"), "查询制度", "fingerprint");
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

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("2026-07-13T08:00:01Z"), ZoneOffset.UTC);
    }
}
