package com.xiaoding.javaai.knowledge.retrieval.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OllamaKnowledgeEmbeddingModelTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sends_one_batch_with_the_configured_model_and_dimensions() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {
                              "model": "qwen3-embedding:4b",
                              "embeddings": [[0.1, 0.2, 0.3], [0.4, 0.5, 0.6]],
                              "total_duration": 123456,
                              "load_duration": 123,
                              "prompt_eval_count": 18
                            }
                            """));

            var model = new OllamaKnowledgeEmbeddingModel(
                    server.url("/").toString(), "qwen3-embedding:4b", 3, objectMapper
            );

            var embeddings = model.embedAll(List.of("退款多久到账", "退款被拒绝怎么办"));

            assertThat(embeddings).hasSize(2);
            assertThat(embeddings).allMatch(value -> value.model().equals("qwen3-embedding:4b"));
            assertThat(embeddings.get(0).vector()).containsExactly(0.1f, 0.2f, 0.3f);
            assertThat(embeddings.get(1).vector()).containsExactly(0.4f, 0.5f, 0.6f);

            var request = server.takeRequest();
            assertThat(request.getMethod()).isEqualTo("POST");
            assertThat(request.getPath()).isEqualTo("/api/embed");
            JsonNode body = objectMapper.readTree(request.getBody().readUtf8());
            assertThat(body.path("model").asText()).isEqualTo("qwen3-embedding:4b");
            assertThat(body.path("dimensions").asInt()).isEqualTo(3);
            assertThat(body.path("truncate").asBoolean()).isTrue();
            assertThat(body.path("input")).hasSize(2);
        }
    }

    @Test
    void rejects_a_response_with_the_wrong_vector_dimensions() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse()
                    .setHeader("Content-Type", "application/json")
                    .setBody("""
                            {"model":"qwen3-embedding:4b","embeddings":[[0.1,0.2]]}
                            """));
            var model = new OllamaKnowledgeEmbeddingModel(
                    server.url("/").toString(), "qwen3-embedding:4b", 3, objectMapper
            );

            assertThatThrownBy(() -> model.embed("退款多久到账"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("dimensions 2 do not match expected 3");
        }
    }
}
