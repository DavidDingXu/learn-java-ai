package com.xiaoding.javaai.ticket.agent.application;

import java.time.Duration;

public interface AgentTelemetry {

    AgentTelemetry NOOP = new AgentTelemetry() {
        @Override
        public void recordPlan(AgentPlanningResult result) {
        }

        @Override
        public void recordTool(String toolName, String outcome, Duration duration) {
        }
    };

    void recordPlan(AgentPlanningResult result);

    void recordTool(String toolName, String outcome, Duration duration);
}
