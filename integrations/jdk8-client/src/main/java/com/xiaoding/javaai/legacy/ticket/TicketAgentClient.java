package com.xiaoding.javaai.legacy.ticket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public final class TicketAgentClient implements AutoCloseable {

    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._:-]{1,128}");

    private final CloseableHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final AccessTokenProvider tokenProvider;

    private TicketAgentClient(
            CloseableHttpClient httpClient,
            ObjectMapper objectMapper,
            String baseUrl,
            AccessTokenProvider tokenProvider
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.tokenProvider = tokenProvider;
    }

    public static TicketAgentClient create(
            TicketAgentClientConfig config,
            AccessTokenProvider tokenProvider
    ) {
        if (config == null) throw new IllegalArgumentException("config must not be null");
        if (tokenProvider == null) throw new IllegalArgumentException("tokenProvider must not be null");
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeoutMillis()))
                .build();
        PoolingHttpClientConnectionManager manager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(connectionConfig)
                        .setMaxConnPerRoute(config.getMaxConnectionsPerRoute())
                        .setMaxConnTotal(config.getMaxConnectionsTotal())
                        .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getConnectTimeoutMillis()))
                .setResponseTimeout(Timeout.ofMilliseconds(config.getResponseTimeoutMillis()))
                .build();
        CloseableHttpClient client = HttpClients.custom()
                .setConnectionManager(manager)
                .setDefaultRequestConfig(requestConfig)
                .disableAutomaticRetries()
                .build();
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return new TicketAgentClient(client, mapper, config.getBaseUrl(), tokenProvider);
    }

    public AgentTaskView getTask(String taskId) {
        String safeTaskId = requireSafeId(taskId, "taskId");
        HttpGet request = new HttpGet(baseUrl + "/api/v1/agent/tasks/" + safeTaskId);
        authorize(request);
        try {
            return httpClient.execute(request, response -> parseResponse(
                    response.getCode(), response.getEntity(), AgentTaskView.class));
        } catch (IOException error) {
            throw new TicketAgentTransportException("unable to read agent task", error);
        }
    }

    public ConfirmationDecisionReceipt confirm(
            String taskId,
            String idempotencyKey,
            ConfirmToolActionRequest command
    ) {
        String safeTaskId = requireSafeId(taskId, "taskId");
        String key = requireIdempotencyKey(idempotencyKey);
        if (command == null) throw new IllegalArgumentException("command must not be null");
        String requestBody = serializeConfirmationRequest(objectMapper, command);
        HttpPut request = new HttpPut(
                baseUrl + "/api/v1/agent/tasks/" + safeTaskId + "/confirmation");
        authorize(request);
        request.setHeader("Idempotency-Key", key);
        request.setEntity(new StringEntity(requestBody, ContentType.APPLICATION_JSON));
        try {
            return httpClient.execute(request, response -> parseResponse(
                    response.getCode(), response.getEntity(), ConfirmationDecisionReceipt.class));
        } catch (IOException error) {
            throw new ConfirmationOutcomeUnknownException(key, error);
        }
    }

    static String serializeConfirmationRequest(
            ObjectMapper objectMapper,
            ConfirmToolActionRequest command
    ) {
        try {
            return objectMapper.writeValueAsString(command);
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("unable to serialize confirmation request", error);
        }
    }

    private void authorize(org.apache.hc.core5.http.ClassicHttpRequest request) {
        String token = tokenProvider.getToken();
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("access token must not be blank");
        }
        request.setHeader("Authorization", "Bearer " + token.trim());
        request.setHeader("Accept", "application/json");
    }

    private <T> T parseResponse(int statusCode, HttpEntity entity, Class<T> responseType)
            throws IOException {
        String body;
        try {
            body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
        } catch (ParseException error) {
            throw new IOException("unable to parse ticket agent response", error);
        }
        if (statusCode >= 200 && statusCode < 300) {
            return objectMapper.readValue(body, responseType);
        }
        ApiError error;
        try {
            error = objectMapper.readValue(body, ApiError.class);
        } catch (Exception ignored) {
            error = new ApiError("HTTP_" + statusCode, "ticket agent request failed");
        }
        boolean retryable = statusCode == 429 || statusCode >= 500;
        throw new TicketAgentClientException(
                statusCode, error.code, error.message, retryable);
    }

    private static String requireSafeId(String value, String name) {
        if (value == null || !SAFE_ID.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException(name + " contains unsupported characters");
        }
        return value.trim();
    }

    private static String requireIdempotencyKey(String value) {
        if (value == null || value.trim().length() < 8 || value.trim().length() > 128) {
            throw new IllegalArgumentException("idempotencyKey length must be between 8 and 128");
        }
        return value.trim();
    }

    @Override
    public void close() {
        try {
            httpClient.close();
        } catch (IOException error) {
            throw new IllegalStateException("unable to close ticket agent client", error);
        }
    }

    private static final class ApiError {
        public String code;
        public String message;

        public ApiError() {
        }

        private ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
