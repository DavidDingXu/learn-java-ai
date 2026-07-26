package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.agent.application.LegacyWriteToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.RemoteExecutionUncertainException;
import com.xiaoding.javaai.ticket.agent.application.ToolExecutionRejectedException;
import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.task.AgentTask;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

public final class HttpLegacyWriteToolExecutor implements LegacyWriteToolExecutor {

    private final RestClient restClient;
    private final DownstreamAccessTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;

    public HttpLegacyWriteToolExecutor(
            RestClient.Builder builder,
            String baseUrl,
            DownstreamAccessTokenProvider tokenProvider,
            Duration timeout
    ) {
        this(builder, baseUrl, tokenProvider, timeout, new ObjectMapper());
    }

    HttpLegacyWriteToolExecutor(
            RestClient.Builder builder,
            String baseUrl,
            DownstreamAccessTokenProvider tokenProvider,
            Duration timeout,
            ObjectMapper objectMapper
    ) {
        this.restClient = builder
                .requestFactory(requestFactory(timeout))
                .baseUrl(requireText(baseUrl, "baseUrl"))
                .build();
        this.tokenProvider = java.util.Objects.requireNonNull(tokenProvider, "tokenProvider must not be null");
        this.objectMapper = java.util.Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    public ToolExecutionReceipt execute(
            AgentTask task,
            ConfirmationRequest confirmation,
            String idempotencyKey
    ) {
        String token = requireText(
                tokenProvider.tokenFor(task, "legacy-tool-service", "legacy:tool:execute"),
                "legacy access token");
        String normalizedIdempotencyKey = requireText(idempotencyKey, "idempotencyKey");
        String requestBody = serializeRequest(new ToolRequest(
                confirmation.actionId(),
                task.request().caseId(),
                confirmation.toolName(),
                confirmation.arguments()));
        try {
            ToolResponse response = restClient.post()
                    .uri("/api/v1/tool-actions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> {
                        headers.setBearerAuth(token);
                        headers.set("Idempotency-Key", normalizedIdempotencyKey);
                    })
                    .body(requestBody)
                    .retrieve()
                    .body(ToolResponse.class);
            if (response == null) {
                throw new RemoteExecutionUncertainException(
                        "legacy tool returned an empty response after accepting the request");
            }
            if (!confirmation.actionId().equals(response.actionId())) {
                throw new RemoteExecutionUncertainException(
                        "legacy tool response actionId does not match the requested action");
            }
            return new ToolExecutionReceipt(
                    response.actionId(), response.status(), response.duplicate(), response.auditId());
        } catch (HttpClientErrorException error) {
            LegacyToolError details = rejectionDetails(error);
            throw new ToolExecutionRejectedException(
                    details.code(), details.message());
        } catch (HttpServerErrorException error) {
            throw new RemoteExecutionUncertainException(
                    "legacy tool outcome is unknown after HTTP " + error.getStatusCode().value());
        } catch (ResourceAccessException error) {
            throw new RemoteExecutionUncertainException(
                    "legacy tool outcome is unknown after transport failure: " + error.getMessage());
        } catch (RemoteExecutionUncertainException error) {
            throw error;
        } catch (RestClientException | IllegalArgumentException error) {
            throw new RemoteExecutionUncertainException(
                    "legacy tool outcome is unknown because the response could not be validated");
        }
    }

    private String serializeRequest(ToolRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("unable to serialize legacy tool request", error);
        }
    }

    private LegacyToolError rejectionDetails(HttpClientErrorException error) {
        String body = error.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            try {
                LegacyToolError parsed = objectMapper.readValue(body, LegacyToolError.class);
                if (hasText(parsed.code()) && hasText(parsed.message())) {
                    return new LegacyToolError(
                            sanitize(parsed.code(), 64), sanitize(parsed.message(), 512));
                }
            } catch (JsonProcessingException ignored) {
                // The fallback below deliberately avoids exposing an untrusted response body.
            }
        }
        return new LegacyToolError(
                "LEGACY_HTTP_" + error.getStatusCode().value(),
                "legacy tool rejected the request");
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String sanitize(String value, int maxLength) {
        String singleLine = value.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= maxLength ? singleLine : singleLine.substring(0, maxLength);
    }

    private static JdkClientHttpRequestFactory requestFactory(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
        factory.setReadTimeout(timeout);
        return factory;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }

    private record ToolRequest(
            String actionId,
            String ticketId,
            String actionType,
            Map<String, String> arguments
    ) {
    }

    private record ToolResponse(
            String actionId,
            String status,
            boolean duplicate,
            String auditId
    ) {
    }

    private record LegacyToolError(String code, String message) {
    }
}
