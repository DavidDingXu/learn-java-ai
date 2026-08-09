package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentReadToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.agent.application.ReadToolUnavailableException;
import com.xiaoding.javaai.ticket.agent.domain.PreparedToolCall;
import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;
import com.xiaoding.javaai.ticket.task.AgentTask;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.ResourceAccessException;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpKnowledgeReadToolExecutor implements AgentReadToolExecutor {

    private final RestClient restClient;
    private final DownstreamAccessTokenProvider tokenProvider;
    private final Clock clock;

    public HttpKnowledgeReadToolExecutor(
            RestClient.Builder builder,
            String baseUrl,
            DownstreamAccessTokenProvider tokenProvider,
            Duration timeout,
            Clock clock
    ) {
        this.restClient = builder
                .requestFactory(requestFactory(timeout))
                .baseUrl(requireText(baseUrl, "baseUrl"))
                .build();
        this.tokenProvider = java.util.Objects.requireNonNull(tokenProvider, "tokenProvider must not be null");
        this.clock = java.util.Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public ToolObservation execute(PreparedToolCall call, AgentTask task) {
        if (!"QUERY_KNOWLEDGE".equals(call.toolName())) {
            throw new IllegalArgumentException("unsupported read tool: " + call.toolName());
        }
        String token = requireText(
                tokenProvider.tokenFor(task, "knowledge-service", "knowledge:answer"),
                "knowledge access token");
        KnowledgeResponse response;
        try {
            response = restClient.post()
                    .uri("/api/v1/knowledge/answers")
                    .headers(headers -> headers.setBearerAuth(token))
                    .body(new KnowledgeRequest(call.arguments().get("question")))
                    .retrieve()
                    .body(KnowledgeResponse.class);
        } catch (HttpClientErrorException error) {
            ReadToolUnavailableException.FailureKind kind = error.getStatusCode().value() == 408
                    || error.getStatusCode().value() == 429
                    ? ReadToolUnavailableException.FailureKind.DEPENDENCY_UNAVAILABLE
                    : ReadToolUnavailableException.FailureKind.REQUEST_REJECTED;
            throw new ReadToolUnavailableException(kind, error);
        } catch (HttpServerErrorException error) {
            throw new ReadToolUnavailableException(
                    ReadToolUnavailableException.FailureKind.DEPENDENCY_UNAVAILABLE, error);
        } catch (ResourceAccessException error) {
            throw new ReadToolUnavailableException(
                    ReadToolUnavailableException.FailureKind.TRANSPORT_FAILURE, error);
        } catch (RestClientException error) {
            throw new ReadToolUnavailableException(
                    ReadToolUnavailableException.FailureKind.INVALID_RESPONSE, error);
        }
        validateResponse(response);
        Map<String, String> result = new LinkedHashMap<>();
        result.put("answer", response.answer() == null ? "" : response.answer());
        result.put("refused", Boolean.toString(response.refused()));
        if (response.refusalReason() != null && !response.refusalReason().isBlank()) {
            result.put("refusalReason", response.refusalReason());
        }
        result.put("citations", response.citations() == null ? "" : response.citations().stream()
                .map(citation -> citation.documentId() + "/" + citation.version() + "#" + citation.sectionId())
                .collect(Collectors.joining(",")));
        result.put("traceId", response.traceId() == null ? "" : response.traceId());
        return new ToolObservation(call.toolName(), result, clock.instant());
    }

    private static void validateResponse(KnowledgeResponse response) {
        if (response == null) {
            throw invalidResponse("knowledge service returned an empty response");
        }
        if (response.citations() == null) {
            throw invalidResponse("knowledge service response omitted citations");
        }
        if (response.refused()) {
            if (!hasText(response.refusalReason())) {
                throw invalidResponse("knowledge refusal omitted its reason");
            }
        } else if (!hasText(response.answer())) {
            throw invalidResponse("knowledge response omitted its answer");
        }
        if (!hasText(response.traceId())) {
            throw invalidResponse("knowledge response omitted traceId");
        }
        for (CitationResponse citation : response.citations()) {
            if (citation == null || !hasText(citation.documentId())
                    || !hasText(citation.version()) || !hasText(citation.sectionId())) {
                throw invalidResponse("knowledge response contained an invalid citation");
            }
        }
    }

    private static ReadToolUnavailableException invalidResponse(String message) {
        return new ReadToolUnavailableException(
                ReadToolUnavailableException.FailureKind.INVALID_RESPONSE,
                new IllegalArgumentException(message));
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static JdkClientHttpRequestFactory requestFactory(Duration timeout) {
        validateTimeout(timeout);
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(timeout);
        return factory;
    }

    private static void validateTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }

    private record KnowledgeRequest(String question) {
    }

    private record KnowledgeResponse(
            String answer,
            List<CitationResponse> citations,
            boolean refused,
            String refusalReason,
            String traceId
    ) {
    }

    private record CitationResponse(
            String documentId,
            String version,
            String sectionId,
            String title
    ) {
    }
}
