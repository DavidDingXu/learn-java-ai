package com.xiaoding.javaai.eval.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class ContractFixtureServer implements AutoCloseable {

    private final HttpServer server;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContractFixtureServer(HttpServer server) {
        this.server = server;
    }

    public static ContractFixtureServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            ContractFixtureServer fixture = new ContractFixtureServer(server);
            server.createContext("/api/v1/knowledge/answers", fixture::answer);
            server.start();
            return fixture;
        } catch (IOException exception) {
            throw new IllegalStateException("failed to start contract fixture server", exception);
        }
    }

    public URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private void answer(com.sun.net.httpserver.HttpExchange exchange) throws IOException {
        try {
            JsonNode request = objectMapper.readTree(exchange.getRequestBody());
            String question = request.path("question").asText();
            boolean refused = question.contains("订单状态")
                    || question.contains("验证码")
                    || question.contains("重新发起退款");
            String answer = refused
                    ? "现有退款制度无法确认订单状态或处理敏感凭证，请联系人工客服核验。"
                    : "退款审核通过后，通常还需要 1 到 5 个工作日原路到账。";
            List<Map<String, String>> citations = refused
                    ? List.of()
                    : List.of(Map.of(
                            "documentId", "refund-policy",
                            "version", "v1",
                            "sectionId", "arrival-time",
                            "title", "退款到账时间"
                    ));
            byte[] body = objectMapper.writeValueAsBytes(Map.of(
                    "answer", answer,
                    "citations", citations,
                    "refused", refused,
                    "refusalReason", refused ? "evidence_missing" : "",
                    "model", "contract-fixture-model",
                    "executionMode", "PROVIDER_PROTOCOL_FIXTURE",
                    "traceId", "fixture-" + Integer.toHexString(question.hashCode()),
                    "usage", Map.of("promptTokens", 30, "completionTokens", 15, "totalTokens", 45)
            ));
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
