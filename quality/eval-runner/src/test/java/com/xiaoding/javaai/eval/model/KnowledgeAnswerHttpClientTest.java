package com.xiaoding.javaai.eval.model;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeAnswerHttpClientTest {

    @Test
    void addsTheDelegatedBearerTokenToEveryKnowledgeRequest() {
        KnowledgeAnswerHttpClient client = new KnowledgeAnswerHttpClient("delegated-token");

        HttpRequest request = client.buildRequest(URI.create("https://knowledge.example"), "退款多久？");

        assertEquals("Bearer delegated-token", request.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void parsesThePublicKnowledgeAnswerContract() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/knowledge/answers", exchange -> {
            byte[] body = """
                    {"answer":"退款通常 1 到 5 个工作日到账。","citations":[{"sectionId":"arrival-time"}],"refused":false,"refusalReason":"","model":"fixture-model","executionMode":"PROVIDER_PROTOCOL_FIXTURE","traceId":"trace-1","usage":{"promptTokens":12,"completionTokens":8,"totalTokens":20}}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            KnowledgeAnswerSnapshot answer = new KnowledgeAnswerHttpClient().answer(
                    URI.create("http://localhost:" + server.getAddress().getPort()), "退款多久？"
            );
            assertEquals("fixture-model", answer.model());
            assertEquals(List.of("arrival-time"), answer.citationSectionIds());
            assertEquals(20, answer.totalTokens());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void exposesAStableErrorForNonSuccessfulHttpResponses() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/v1/knowledge/answers", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            KnowledgeAnswerClientException error = assertThrows(
                    KnowledgeAnswerClientException.class,
                    () -> new KnowledgeAnswerHttpClient().answer(
                            URI.create("http://localhost:" + server.getAddress().getPort()), "问题")
            );
            assertTrue(error.getMessage().contains("503"));
        } finally {
            server.stop(0);
        }
    }
}
