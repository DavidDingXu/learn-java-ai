package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaoding.javaai.ticket.agent.application.AgentPlanningContext;
import com.xiaoding.javaai.ticket.agent.application.AgentPlanningResult;
import com.xiaoding.javaai.ticket.agent.application.TicketAgentPlanner;
import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TicketAgentProviderProtocolFixtureTest {

    private static final MockWebServer PROVIDER = new MockWebServer();

    @BeforeAll
    static void startProvider() throws IOException {
        PROVIDER.start();
    }

    @AfterAll
    static void stopProvider() throws IOException {
        PROVIDER.shutdown();
    }

    @DynamicPropertySource
    static void providerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.model.chat", () -> "openai");
        registry.add("spring.ai.openai.api-key", () -> "fixture-key");
        registry.add("spring.ai.openai.base-url", () -> PROVIDER.url("/").toString());
        registry.add("spring.ai.openai.chat.model", () -> "fixture-model");
    }

    @Autowired
    private TicketAgentPlanner planner;

    @Test
    void maps_an_openai_compatible_structured_tool_selection() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String structured = mapper.writeValueAsString(Map.of(
                "decisionType", "USE_TOOL",
                "toolName", "QUERY_KNOWLEDGE",
                "arguments", Map.of("question", "退款多久到账？"),
                "rationale", "需要查询当前退款制度",
                "summary", "",
                "reasonCode", "",
                "message", ""
        ));
        String providerResponse = mapper.writeValueAsString(Map.of(
                "id", "chatcmpl-agent-fixture",
                "object", "chat.completion",
                "created", 1750000000,
                "model", "fixture-model",
                "choices", List.of(Map.of(
                        "index", 0,
                        "message", Map.of("role", "assistant", "content", structured),
                        "finish_reason", "stop"
                )),
                "usage", Map.of("prompt_tokens", 80, "completion_tokens", 30, "total_tokens", 110)
        ));
        PROVIDER.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(providerResponse));

        AgentPlanningResult result = planner.plan(new AgentPlanningContext(
                "task-100",
                "Resolve customer consultation",
                Map.of("question", "退款多久到账？"),
                List.of(),
                Map.of(
                        "QUERY_KNOWLEDGE", Set.of("question"),
                        "ASSIGN_QUEUE", Set.of("queueCode")),
                0));

        assertThat(result.decision()).isInstanceOfSatisfying(AgentDecision.UseTool.class, useTool -> {
            assertThat(useTool.toolName()).isEqualTo("QUERY_KNOWLEDGE");
            assertThat(useTool.arguments()).containsEntry("question", "退款多久到账？");
        });
        assertThat(result.model()).isEqualTo("fixture-model");
        assertThat(result.usage().totalTokens()).isEqualTo(110);
        assertThat(result.finishReason()).isEqualTo("stop");
        RecordedRequest request = PROVIDER.takeRequest(2, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/chat/completions");
        assertThat(request.getBody().readUtf8())
                .contains("UNTRUSTED_TASK_OBJECTIVE")
                .contains("SERVER_TOOL_POLICY")
                .contains("QUERY_KNOWLEDGE");
    }
}
