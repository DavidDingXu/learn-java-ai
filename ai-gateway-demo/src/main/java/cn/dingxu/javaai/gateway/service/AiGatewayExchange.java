package cn.dingxu.javaai.gateway.service;

import cn.dingxu.javaai.common.model.AiChatRequest;
import cn.dingxu.javaai.common.trace.AiTrace;

import java.util.ArrayList;
import java.util.List;

public record AiGatewayExchange(
        AiTrace trace,
        AiChatRequest request,
        String prompt,
        List<String> advisorEvents
) {
    public AiGatewayExchange {
        advisorEvents = advisorEvents == null ? List.of() : List.copyOf(advisorEvents);
    }

    public static AiGatewayExchange start(AiTrace trace, AiChatRequest request, String prompt) {
        return new AiGatewayExchange(trace, request, prompt, List.of());
    }

    public AiGatewayExchange appendPrompt(String extraPrompt) {
        if (extraPrompt == null || extraPrompt.isBlank()) {
            return this;
        }
        return new AiGatewayExchange(trace, request, prompt + extraPrompt, advisorEvents);
    }

    public AiGatewayExchange addAdvisorEvent(String advisorName, String action, String detail) {
        List<String> events = new ArrayList<>(advisorEvents);
        events.add(advisorName + ":" + action);
        return new AiGatewayExchange(trace, request, prompt, events);
    }
}
