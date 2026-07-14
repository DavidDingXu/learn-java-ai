package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.TicketAgentPlanner;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
class TicketAgentPlannerConfiguration {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
    TicketAgentPlanner disabledTicketAgentPlanner() {
        return new DisabledTicketAgentPlanner();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
    TicketAgentPlanner springAiTicketAgentPlanner(
            ChatClient.Builder builder,
            @org.springframework.beans.factory.annotation.Value(
                    "${spring.ai.openai.chat.model:unknown}") String configuredModel,
            @org.springframework.beans.factory.annotation.Value(
                    "classpath:prompts/ticket-agent/v1/system.txt") Resource systemPrompt
    ) throws IOException {
        return new SpringAiTicketAgentPlanner(
                builder,
                configuredModel,
                systemPrompt.getContentAsString(StandardCharsets.UTF_8));
    }
}
