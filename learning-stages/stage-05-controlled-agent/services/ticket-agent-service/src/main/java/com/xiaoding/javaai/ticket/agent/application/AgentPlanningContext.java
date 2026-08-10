package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.ToolObservation;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record AgentPlanningContext(
        String taskId,
        String objective,
        Map<String, String> businessContext,
        List<ToolObservation> observations,
        Map<String, Set<String>> availableTools,
        int step
) {
    public AgentPlanningContext {
        businessContext = Map.copyOf(businessContext);
        observations = List.copyOf(observations);
        availableTools = availableTools.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> Set.copyOf(entry.getValue())
        ));
    }
}
