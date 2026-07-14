package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentPlanningResult;
import com.xiaoding.javaai.ticket.agent.application.AgentTelemetry;
import com.xiaoding.javaai.ticket.agent.domain.AgentDecision;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

final class MicrometerAgentTelemetry implements AgentTelemetry {

    private final MeterRegistry registry;

    MicrometerAgentTelemetry(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    @Override
    public void recordPlan(AgentPlanningResult result) {
        String decision = decisionType(result).toLowerCase(Locale.ROOT);
        String finishReason = normalize(result.finishReason());
        Counter.builder("java.ai.agent.plan")
                .tag("decision", decision)
                .tag("finish_reason", finishReason)
                .register(registry)
                .increment();
        DistributionSummary.builder("java.ai.agent.plan.tokens")
                .tag("decision", decision)
                .register(registry)
                .record(result.usage().totalTokens());
    }

    @Override
    public void recordTool(String toolName, String outcome, Duration duration) {
        Timer.builder("java.ai.agent.tool")
                .tag("tool", normalize(toolName).toUpperCase(Locale.ROOT))
                .tag("outcome", normalize(outcome))
                .register(registry)
                .record(duration);
    }

    private static String decisionType(AgentPlanningResult result) {
        if (result.decision() instanceof AgentDecision.UseTool) return "USE_TOOL";
        if (result.decision() instanceof AgentDecision.Finish) return "FINISH";
        return "REFUSE";
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) return "unknown";
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.:-]", "_");
    }
}
