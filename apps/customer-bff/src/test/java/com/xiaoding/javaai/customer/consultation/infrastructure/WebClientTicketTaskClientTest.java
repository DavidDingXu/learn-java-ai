package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.domain.CitationView;
import com.xiaoding.javaai.customer.consultation.domain.TicketHandoffSnapshot;
import com.xiaoding.javaai.customer.downstream.DownstreamServiceException;
import com.xiaoding.javaai.customer.downstream.DownstreamTimeoutException;
import com.xiaoding.javaai.customer.identity.DelegatedAccessToken;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebClientTicketTaskClientTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    void creates_an_idempotent_task_without_copying_authorization_facts_into_the_body() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"taskId\":\"task-100\",\"status\":\"ACCEPTED\",\"duplicate\":false}"));
        WebClientTicketTaskClient client = new WebClientTicketTaskClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofSeconds(2));
        TicketHandoffSnapshot snapshot = new TicketHandoffSnapshot(
                "conversation-1", "attempt-1", "tenant-a", "customer-42",
                "退款多久到账？", "退款通常在 1 到 5 个工作日到账。",
                List.of(new CitationView("refund-policy", "v1", "arrival-time", "退款到账时间")),
                null, null, "", "CUSTOMER_REQUESTED_HUMAN",
                "handoff:tenant-a:conversation-1:attempt-1", "trace-123",
                Instant.parse("2026-07-13T04:00:00Z")
        );

        var receipt = client.createHandoff(
                new DelegatedAccessToken("ticket-token", Instant.now().plusSeconds(60)),
                snapshot.idempotencyKey(), snapshot).block();

        var request = server.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(request.getPath()).isEqualTo("/api/v1/agent/tasks");
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer ticket-token");
        assertThat(request.getHeader("Idempotency-Key")).isEqualTo(snapshot.idempotencyKey());
        assertThat(body)
                .contains("\"caseId\":\"conversation-1\"")
                .contains("\"sourceTraceId\":\"trace-123\"")
                .doesNotContain("tenant-a")
                .doesNotContain("customer-42");
        assertThat(receipt.taskId()).isEqualTo("task-100");
    }

    @Test
    void mapsTheClientDeadlineToAStableDownstreamTimeout() {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody("{}")
                .setBodyDelay(250, TimeUnit.MILLISECONDS));
        WebClientTicketTaskClient client = new WebClientTicketTaskClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofMillis(25));
        TicketHandoffSnapshot snapshot = new TicketHandoffSnapshot(
                "conversation-1", "attempt-1", "tenant-a", "customer-42",
                "退款多久到账？", "请转人工处理。", List.of(),
                null, null, "", "CUSTOMER_REQUESTED_HUMAN",
                "handoff:tenant-a:conversation-1:attempt-1", "trace-123", Instant.now()
        );

        assertThatThrownBy(() -> client.createHandoff(
                new DelegatedAccessToken("ticket-token", Instant.now().plusSeconds(60)),
                snapshot.idempotencyKey(), snapshot).block())
                .isInstanceOf(DownstreamTimeoutException.class)
                .hasMessage("Downstream service timed out");
    }

    @Test
    void mapsInvalidSuccessfulResponseToAStableDownstreamError() {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json")
                .setBody("{\"taskId\":\"\",\"status\":\"ACCEPTED\",\"duplicate\":false}"));
        WebClientTicketTaskClient client = new WebClientTicketTaskClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.createHandoff(
                new DelegatedAccessToken("ticket-token", Instant.now().plusSeconds(60)),
                snapshot().idempotencyKey(), snapshot()).block())
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessage("Downstream service request failed");
    }

    @Test
    void mapsEmptySuccessfulResponseToAStableDownstreamError() {
        server.enqueue(new MockResponse()
                .setResponseCode(202)
                .setHeader("Content-Type", "application/json"));
        WebClientTicketTaskClient client = new WebClientTicketTaskClient(
                WebClient.builder(), server.url("/").toString(), Duration.ofSeconds(2));

        assertThatThrownBy(() -> client.createHandoff(
                new DelegatedAccessToken("ticket-token", Instant.now().plusSeconds(60)),
                snapshot().idempotencyKey(), snapshot()).block())
                .isInstanceOf(DownstreamServiceException.class)
                .hasMessage("Downstream service request failed");
    }

    private static TicketHandoffSnapshot snapshot() {
        return new TicketHandoffSnapshot(
                "conversation-1", "attempt-1", "tenant-a", "customer-42",
                "退款多久到账？", "请转人工处理。", List.of(),
                null, null, "", "CUSTOMER_REQUESTED_HUMAN",
                "handoff:tenant-a:conversation-1:attempt-1", "trace-123", Instant.now()
        );
    }
}
