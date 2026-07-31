package com.xiaoding.javaai.customer.consultation.infrastructure;

import com.xiaoding.javaai.customer.consultation.application.CustomerConsultationService;
import com.xiaoding.javaai.customer.consultation.application.port.ConsultationRateLimiter;
import com.xiaoding.javaai.customer.consultation.application.port.ConsultationSessionStore;
import com.xiaoding.javaai.customer.consultation.application.port.IdGenerator;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerClient;
import com.xiaoding.javaai.customer.consultation.application.port.KnowledgeAnswerStreamClient;
import com.xiaoding.javaai.customer.consultation.application.port.TicketTaskClient;
import com.xiaoding.javaai.customer.consultation.domain.ConversationWindowPolicy;
import com.xiaoding.javaai.customer.identity.CustomerJwtIdentityFactory;
import com.xiaoding.javaai.customer.identity.DelegatedTokenClient;
import com.xiaoding.javaai.customer.identity.OAuth2TokenExchangeDelegatedTokenClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Configuration
public class CustomerConsultationConfiguration {

    @Bean
    Clock customerConsultationClock() {
        return Clock.systemUTC();
    }

    @Bean
    CustomerJwtIdentityFactory customerJwtIdentityFactory() {
        return new CustomerJwtIdentityFactory();
    }

    @Bean
    ConsultationSessionStore consultationSessionStore() {
        return new InMemoryConsultationSessionStore();
    }

    @Bean
    IdGenerator consultationIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    ConversationWindowPolicy conversationWindowPolicy(
            @Value("${java-ai.consultation.max-turns:8}") int maxTurns,
            @Value("${java-ai.consultation.max-estimated-tokens:1200}") int maxTokens,
            @Value("${java-ai.consultation.max-summary-chars:800}") int maxSummaryChars
    ) {
        return new ConversationWindowPolicy(maxTurns, maxTokens, maxSummaryChars);
    }

    @Bean
    ConsultationRateLimiter consultationRateLimiter(
            @Value("${java-ai.consultation.rate-limit.requests:20}") int requests,
            @Value("${java-ai.consultation.rate-limit.window:1m}") Duration window
    ) {
        return new InMemoryFixedWindowConsultationRateLimiter(requests, window);
    }

    @Bean
    KnowledgeAnswerClient knowledgeAnswerClient(
            WebClient.Builder builder,
            @Value("${java-ai.runtime.external-integrations-enabled:false}") boolean enabled,
            @Value("${java-ai.downstream.knowledge.base-url:http://localhost:8081}") String baseUrl,
            @Value("${java-ai.downstream.knowledge.timeout:35s}") Duration timeout
    ) {
        if (!enabled) return (token, request) -> Mono.error(
                new IllegalStateException("knowledge-service integration is disabled"));
        return new WebClientKnowledgeAnswerClient(builder, baseUrl, timeout);
    }

    @Bean
    KnowledgeAnswerStreamClient knowledgeAnswerStreamClient(
            WebClient.Builder builder,
            @Value("${java-ai.runtime.external-integrations-enabled:false}") boolean enabled,
            @Value("${java-ai.downstream.knowledge.base-url:http://localhost:8081}") String baseUrl,
            @Value("${java-ai.downstream.knowledge.stream-idle-timeout:30s}") Duration idleTimeout,
            @Value("${java-ai.downstream.knowledge.stream-total-timeout:2m}") Duration totalTimeout
    ) {
        if (!enabled) return (token, request) -> reactor.core.publisher.Flux.error(
                new IllegalStateException("knowledge-service stream integration is disabled"));
        return new WebClientKnowledgeAnswerStreamClient(
                builder, baseUrl, idleTimeout, totalTimeout
        );
    }

    @Bean
    TicketTaskClient ticketTaskClient(
            WebClient.Builder builder,
            @Value("${java-ai.runtime.external-integrations-enabled:false}") boolean enabled,
            @Value("${java-ai.downstream.ticket.base-url:http://localhost:8082}") String baseUrl,
            @Value("${java-ai.downstream.ticket.timeout:5s}") Duration timeout
    ) {
        if (!enabled) return (token, key, snapshot) -> Mono.error(
                new IllegalStateException("ticket-agent-service integration is disabled"));
        return new WebClientTicketTaskClient(builder, baseUrl, timeout);
    }

    @Bean
    @Qualifier("knowledgeDelegatedTokenClient")
    DelegatedTokenClient knowledgeDelegatedTokenClient(
            WebClient.Builder builder,
            @Value("${java-ai.runtime.external-integrations-enabled:false}") boolean enabled,
            @Value("${java-ai.identity.token-endpoint:http://localhost/disabled}") String endpoint,
            @Value("${java-ai.identity.client-id:}") String clientId,
            @Value("${java-ai.identity.client-secret:}") String clientSecret,
            @Value("${java-ai.identity.timeout:5s}") Duration timeout
    ) {
        if (!enabled) return disabledTokenClient();
        return new OAuth2TokenExchangeDelegatedTokenClient(
                builder, endpoint, "knowledge-service", "knowledge:answer",
                clientId, clientSecret, timeout);
    }

    @Bean
    @Qualifier("ticketDelegatedTokenClient")
    DelegatedTokenClient ticketDelegatedTokenClient(
            WebClient.Builder builder,
            @Value("${java-ai.runtime.external-integrations-enabled:false}") boolean enabled,
            @Value("${java-ai.identity.token-endpoint:http://localhost/disabled}") String endpoint,
            @Value("${java-ai.identity.client-id:}") String clientId,
            @Value("${java-ai.identity.client-secret:}") String clientSecret,
            @Value("${java-ai.identity.timeout:5s}") Duration timeout
    ) {
        if (!enabled) return disabledTokenClient();
        return new OAuth2TokenExchangeDelegatedTokenClient(
                builder, endpoint, "ticket-agent-service", "ticket:task:create",
                clientId, clientSecret, timeout);
    }

    @Bean
    CustomerConsultationService customerConsultationService(
            ConsultationSessionStore store,
            KnowledgeAnswerClient knowledgeClient,
            KnowledgeAnswerStreamClient streamClient,
            TicketTaskClient ticketClient,
            @Qualifier("knowledgeDelegatedTokenClient") DelegatedTokenClient knowledgeTokens,
            @Qualifier("ticketDelegatedTokenClient") DelegatedTokenClient ticketTokens,
            ConsultationRateLimiter rateLimiter,
            ConversationWindowPolicy windowPolicy,
            IdGenerator idGenerator,
            Clock customerConsultationClock,
            @Value("${java-ai.consultation.session-ttl:30m}") Duration sessionTtl
    ) {
        return new CustomerConsultationService(
                store, knowledgeClient, streamClient, ticketClient, knowledgeTokens, ticketTokens,
                rateLimiter, windowPolicy, idGenerator, customerConsultationClock, sessionTtl);
    }

    private static DelegatedTokenClient disabledTokenClient() {
        return source -> Mono.error(new IllegalStateException("delegated identity integration is disabled"));
    }
}
