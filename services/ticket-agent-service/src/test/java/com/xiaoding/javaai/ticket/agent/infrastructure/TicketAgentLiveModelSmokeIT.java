package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentPlanningContext;
import com.xiaoding.javaai.ticket.agent.application.AgentPlanningResult;
import com.xiaoding.javaai.ticket.agent.application.TicketAgentPlanner;
import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class TicketAgentLiveModelSmokeIT {

    @DynamicPropertySource
    static void liveModelProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.model.chat", () -> "openai");
        registry.add("java-ai.runtime.external-integrations-enabled", () -> true);
    }

    @Autowired
    private TicketAgentPlanner planner;

    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @BeforeEach
    void requireConfiguredApiKey() {
        if (apiKey.isBlank() || "replace-with-your-api-key".equals(apiKey)) {
            throw new IllegalStateException("请先在 config/application.yml 中填写 spring.ai.openai.api-key");
        }
    }

    @Test
    void callsTheConfiguredModelAndWritesRedactedPlanningEvidence() throws IOException {
        AgentPlanningResult result = planner.plan(new AgentPlanningContext(
                "smoke-task",
                "Resolve a customer question about the current refund arrival policy",
                Map.of("question", "退款审核通过后多久到账？"),
                List.of(),
                Set.of("QUERY_KNOWLEDGE", "ASSIGN_QUEUE", "REQUEST_MANUAL_REVIEW"),
                0));

        assertThat(result.model()).isNotBlank();
        assertThat(result.finishReason()).isNotBlank();
        assertThat(result.usage().totalTokens()).isGreaterThan(0);
        assertThat(result.decision()).isInstanceOfSatisfying(AgentDecision.UseTool.class, decision ->
                assertThat(decision.toolName()).isEqualTo("QUERY_KNOWLEDGE"));

        writeReport(result);
    }

    private static void writeReport(AgentPlanningResult result) throws IOException {
        Path reportPath = Path.of(requiredSystemProperty("java-ai.agent-smoke.report-path"));
        Path parent = reportPath.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        AgentDecision.UseTool decision = (AgentDecision.UseTool) result.decision();
        String report = """
                # Ticket Agent Live Model Smoke

                Status: LIVE_MODEL

                - Executed at: %s
                - Commit: `%s`
                - Spring AI: `2.0.0`
                - Model: `%s`
                - Finish reason: `%s`
                - Prompt tokens: %d
                - Completion tokens: %d
                - Total tokens: %d
                - Decision type: `USE_TOOL`
                - Selected tool: `%s`

                The report excludes the API key, provider base URL, prompt body and customer context.
                """.formatted(
                Instant.now(),
                System.getProperty("java-ai.agent-smoke.commit", "unknown"),
                result.model(),
                result.finishReason(),
                result.usage().promptTokens(),
                result.usage().completionTokens(),
                result.usage().totalTokens(),
                decision.toolName());
        Files.writeString(reportPath, report, StandardCharsets.UTF_8);
    }

    private static String requiredSystemProperty(String name) {
        String value = System.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required system property: " + name);
        }
        return value;
    }

}
