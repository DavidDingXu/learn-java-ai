package com.xiaoding.javaai.ticket.agent.infrastructure;

import com.xiaoding.javaai.ticket.agent.application.AgentReadToolExecutor;
import com.xiaoding.javaai.ticket.agent.application.DownstreamAccessTokenProvider;
import com.xiaoding.javaai.ticket.agent.application.LegacyWriteToolExecutor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(AgentDownstreamProperties.class)
class AgentDownstreamConfiguration {

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.agent.downstream-enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    AgentReadToolExecutor disabledAgentReadToolExecutor() {
        return (call, task) -> {
            throw new AgentExternalIntegrationDisabledException("knowledge read tool");
        };
    }

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.agent.downstream-enabled",
            havingValue = "false",
            matchIfMissing = true
    )
    LegacyWriteToolExecutor disabledLegacyWriteToolExecutor() {
        return (task, confirmation, idempotencyKey) -> {
            throw new AgentExternalIntegrationDisabledException("legacy write tool");
        };
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.agent.downstream-enabled", havingValue = "true")
    AgentReadToolExecutor httpKnowledgeReadToolExecutor(
            RestClient.Builder builder,
            DownstreamAccessTokenProvider tokenProvider,
            Clock ticketClock,
            AgentDownstreamProperties properties
    ) {
        return new HttpKnowledgeReadToolExecutor(
                builder, properties.getKnowledgeBaseUrl(), tokenProvider,
                properties.getDownstreamTimeout(), ticketClock);
    }

    @Bean
    @ConditionalOnProperty(name = "java-ai.agent.downstream-enabled", havingValue = "true")
    LegacyWriteToolExecutor httpLegacyWriteToolExecutor(
            RestClient.Builder builder,
            DownstreamAccessTokenProvider tokenProvider,
            AgentDownstreamProperties properties
    ) {
        return new HttpLegacyWriteToolExecutor(
                builder, properties.getLegacyToolBaseUrl(), tokenProvider,
                properties.getDownstreamTimeout());
    }

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.agent.downstream-token-mode",
            havingValue = "development-hmac"
    )
    DownstreamAccessTokenProvider developmentHmacDownstreamAccessTokenProvider(
            Clock ticketClock,
            AgentDownstreamProperties properties
    ) {
        AgentDownstreamProperties.DelegatedToken token = properties.getDelegatedToken();
        return new DevelopmentHmacDownstreamAccessTokenProvider(
                token.getIssuer(), token.getHmacSecret(), token.getActorId(), token.getTtl(), ticketClock,
                () -> UUID.randomUUID().toString());
    }
}
