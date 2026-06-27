package com.xiaoding.javaai.agent.service.springai;

import java.util.List;
import java.util.Map;

public record SpringAiAdvisorBinding(
        String conversationId,
        Map<String, Object> advisorParams,
        List<String> sources,
        int totalEstimatedTokens
) {
    public SpringAiAdvisorBinding {
        advisorParams = Map.copyOf(advisorParams);
        sources = List.copyOf(sources);
    }
}
