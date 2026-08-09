package com.xiaoding.javaai.stages.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

public final class StageHttp {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonNode get(String url) {
        return exchange(HttpRequest.newBuilder(URI.create(url)).GET().build());
    }

    public JsonNode post(String url, Object body) {
        return json("POST", url, body, Map.of());
    }

    public JsonNode post(String url, Object body, Map<String, String> headers) {
        return json("POST", url, body, headers);
    }

    public JsonNode put(String url, Object body, Map<String, String> headers) {
        return json("PUT", url, body, headers);
    }

    public JsonNode postWithoutBody(String url) {
        return exchange(HttpRequest.newBuilder(URI.create(url))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
    }

    public JsonNode upload(
            String url,
            String metadataJson,
            Path file,
            String mediaType
    ) {
        String boundary = "java-ai-stage-" + UUID.randomUUID();
        byte[] prefix = ("--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"metadata\"\r\n"
                + "Content-Type: application/json\r\n\r\n"
                + metadataJson + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\""
                + file.getFileName() + "\"\r\n"
                + "Content-Type: " + mediaType + "\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] suffix = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        try {
            HttpRequest.BodyPublisher body = HttpRequest.BodyPublishers.concat(
                    HttpRequest.BodyPublishers.ofByteArray(prefix),
                    HttpRequest.BodyPublishers.ofFile(file),
                    HttpRequest.BodyPublishers.ofByteArray(suffix)
            );
            return exchange(HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(body)
                    .build());
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read upload file " + file, error);
        }
    }

    public String text(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> response = send(request);
        requireSuccess(response);
        return response.body();
    }

    private JsonNode json(String method, String url, Object body, Map<String, String> headers) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json");
            headers.forEach(request::header);
            request.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            return exchange(request.build());
        } catch (IOException error) {
            throw new IllegalStateException("Cannot serialize request for " + url, error);
        }
    }

    private JsonNode exchange(HttpRequest request) {
        HttpResponse<String> response = send(request);
        requireSuccess(response);
        try {
            return response.body() == null || response.body().isBlank()
                    ? objectMapper.createObjectNode()
                    : objectMapper.readTree(response.body());
        } catch (IOException error) {
            throw new IllegalStateException("Invalid JSON from " + request.uri(), error);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new IllegalStateException("Cannot reach " + request.uri(), error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while calling " + request.uri(), error);
        }
    }

    private static void requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(
                    "HTTP " + response.statusCode() + " from " + response.uri() + ": " + response.body()
            );
        }
    }
}
