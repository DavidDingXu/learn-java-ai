package com.xiaoding.javaai.agent.service.springai;

import com.xiaoding.javaai.agent.service.context.EngineeredContext;
import com.xiaoding.javaai.agent.service.memory.AgentContext;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

@Service
public class SpringAiAdvisorMemoryBridge {

    public ChatMemory windowMemory(int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages must be positive");
        }
        return MessageWindowChatMemory.builder()
                .maxMessages(maxMessages)
                .build();
    }

    public MessageChatMemoryAdvisor memoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(Objects.requireNonNull(chatMemory, "chatMemory must not be null"))
                .build();
    }

    public SpringAiAdvisorBinding bind(AgentContext context, EngineeredContext engineeredContext) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(engineeredContext, "engineeredContext must not be null");

        String conversationId = conversationId(context);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put(ChatMemory.CONVERSATION_ID, conversationId);
        params.put("java-ai.context.sources", engineeredContext.sources());
        params.put("java-ai.context.totalEstimatedTokens", engineeredContext.totalEstimatedTokens());

        return new SpringAiAdvisorBinding(
                conversationId,
                params,
                engineeredContext.sources(),
                engineeredContext.totalEstimatedTokens()
        );
    }

    private String conversationId(AgentContext context) {
        return required(context.tenantId(), "tenantId")
                + ":" + required(context.sessionId(), "sessionId")
                + ":" + required(context.businessId(), "businessId");
    }

    private String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }
}
