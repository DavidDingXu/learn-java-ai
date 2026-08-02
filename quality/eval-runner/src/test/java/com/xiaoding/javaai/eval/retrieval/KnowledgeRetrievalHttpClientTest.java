package com.xiaoding.javaai.eval.retrieval;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeRetrievalHttpClientTest {

    @Test
    void targetsTheInternalEndpointWithTheEvaluationToken() {
        HttpRequest request = new KnowledgeRetrievalHttpClient().buildRequest(
                URI.create("https://knowledge.example.test/"),
                "evaluation-token",
                "退款多久到账？",
                5
        );

        assertEquals(
                "https://knowledge.example.test/internal/v1/knowledge/retrieval/evaluations",
                request.uri().toString()
        );
        assertEquals("Bearer evaluation-token",
                request.headers().firstValue("Authorization").orElseThrow());
    }

    @Test
    void omitsAuthorizationForTheLocalMockIdentity() {
        HttpRequest request = new KnowledgeRetrievalHttpClient().buildRequest(
                URI.create("http://localhost:8081"),
                null,
                "退款多久到账？",
                5
        );

        assertTrue(request.headers().firstValue("Authorization").isEmpty());
    }
}
