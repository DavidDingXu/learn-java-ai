package com.xiaoding.javaai.legacy.agent;

import com.xiaoding.javaai.legacy.agent.model.AgentTaskRequest;
import com.xiaoding.javaai.legacy.agent.model.AgentTaskResult;
import com.xiaoding.javaai.legacy.legacy.model.OperatorContext;
import com.xiaoding.javaai.legacy.legacy.model.TicketSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

public class HttpLegacyAgentClient implements LegacyAgentClient {

    private final URL endpoint;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public HttpLegacyAgentClient(URL endpoint) {
        this(endpoint, 3000, 15000);
    }

    public HttpLegacyAgentClient(URL endpoint, int connectTimeoutMs, int readTimeoutMs) {
        if (endpoint == null) {
            throw new IllegalArgumentException("endpoint must not be null");
        }
        this.endpoint = endpoint;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
    }

    @Override
    public AgentTaskResult requestAdvice(AgentTaskRequest request) {
        try {
            HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            byte[] body = toJson(request).getBytes(StandardCharsets.UTF_8);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int status = connection.getResponseCode();
            String response = readResponse(status >= 400 ? connection.getErrorStream() : connection.getInputStream());
            if (status < 200 || status >= 300) {
                throw new IllegalStateException("agent service returned HTTP " + status + ": " + response);
            }
            return AgentTaskResult.completed(
                    request.getTaskId(),
                    firstNonBlank(extractString(response, "content"), extractString(response, "advice")),
                    requiresHumanApproval(response),
                    firstNonBlank(extractString(response, "traceId"), "remote-" + request.getTaskId()),
                    Collections.<TicketSnapshot>emptyList()
            );
        } catch (IOException error) {
            throw new IllegalStateException("failed to call external agent service: " + endpoint, error);
        }
    }

    private String toJson(AgentTaskRequest request) {
        OperatorContext operator = request.getOperatorContext();
        return "{"
                + "\"ticketId\":\"" + escape(request.getTicketId()) + "\","
                + "\"question\":\"" + escape(request.getQuestion()) + "\","
                + "\"userId\":\"" + escape(operator.getOperatorId()) + "\","
                + "\"tenantId\":\"" + escape(operator.getTenantId()) + "\","
                + "\"department\":\"" + escape(first(operator.getDepartments())) + "\""
                + "}";
    }

    private String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        int read;
        while ((read = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private boolean requiresHumanApproval(String response) {
        String requiredAction = extractString(response, "requiredAction");
        if ("MANUAL_REVIEW".equals(requiredAction)) {
            return true;
        }
        return response.contains("\"requiresHumanApproval\":true");
    }

    private String extractString(String json, String fieldName) {
        String marker = "\"" + fieldName + "\"";
        int fieldIndex = json.indexOf(marker);
        if (fieldIndex < 0) {
            return "";
        }
        int colonIndex = json.indexOf(':', fieldIndex + marker.length());
        if (colonIndex < 0) {
            return "";
        }
        int startQuote = json.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            return "";
        }
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = startQuote + 1; i < json.length(); i++) {
            char current = json.charAt(i);
            if (escaped) {
                value.append(current);
                escaped = false;
            } else if (current == '\\') {
                escaped = true;
            } else if (current == '"') {
                return value.toString();
            } else {
                value.append(current);
            }
        }
        return "";
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.trim().isEmpty()) {
            return first;
        }
        return second == null ? "" : second;
    }

    private String first(Set<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.iterator().next();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
