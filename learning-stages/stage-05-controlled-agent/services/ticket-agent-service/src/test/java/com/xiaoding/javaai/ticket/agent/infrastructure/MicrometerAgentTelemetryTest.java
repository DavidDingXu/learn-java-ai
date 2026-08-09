package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentModelUsage;
import com.xiaoding.javaai.ticket.agent.application.AgentPlanningResult;
import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class MicrometerAgentTelemetryTest {

    @Test
    void records_low_cardinality_plan_and_tool_metrics_without_task_or_prompt_tags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerAgentTelemetry telemetry = new MicrometerAgentTelemetry(registry);

        telemetry.recordPlan(new AgentPlanningResult(
                new AgentDecision.UseTool("QUERY_KNOWLEDGE", java.util.Map.of("question", "secret"), "reason"),
                "provider-model", new AgentModelUsage(100, 20, 120), "stop"));
        telemetry.recordTool("QUERY_KNOWLEDGE", "succeeded", Duration.ofMillis(25));

        assertThat(registry.get("java.ai.agent.plan").tag("decision", "use_tool")
                .tag("finish_reason", "stop").counter().count()).isEqualTo(1.0);
        assertThat(registry.get("java.ai.agent.plan.tokens").tag("decision", "use_tool")
                .summary().totalAmount()).isEqualTo(120.0);
        assertThat(registry.get("java.ai.agent.tool").tag("tool", "QUERY_KNOWLEDGE")
                .tag("outcome", "succeeded").timer().count()).isEqualTo(1L);
        assertThat(registry.getMeters()).allSatisfy(meter ->
                assertThat(meter.getId().getTags())
                        .extracting(tag -> tag.getKey())
                        .doesNotContain("task_id", "prompt", "question", "model"));
    }

    @Test
    void collapses_provider_specific_finish_reasons_to_a_bounded_tag() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerAgentTelemetry telemetry = new MicrometerAgentTelemetry(registry);

        telemetry.recordPlan(new AgentPlanningResult(
                new AgentDecision.Finish("done"),
                "provider-model", AgentModelUsage.unknown(), "provider-stop-7f92"));

        assertThat(registry.get("java.ai.agent.plan")
                .tag("decision", "finish")
                .tag("finish_reason", "other")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.find("java.ai.agent.plan")
                .tag("finish_reason", "provider-stop-7f92")
                .counter()).isNull();
    }
}
