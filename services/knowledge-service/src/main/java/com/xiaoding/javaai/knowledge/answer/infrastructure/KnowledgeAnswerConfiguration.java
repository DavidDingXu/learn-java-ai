package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.AnswerKnowledgeQuestion;
import com.xiaoding.javaai.knowledge.answer.application.KnowledgeAnswerService;
import com.xiaoding.javaai.knowledge.answer.application.StreamKnowledgeAnswer;
import com.xiaoding.javaai.knowledge.answer.application.StreamingKnowledgeAnswerService;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerModel;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerTelemetry;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerStreamModel;
import com.xiaoding.javaai.knowledge.answer.application.port.PolicyContextSource;
import com.xiaoding.javaai.knowledge.answer.application.port.TraceIdProvider;
import io.micrometer.tracing.Tracer;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

@Configuration
class KnowledgeAnswerConfiguration {

    @Bean
    Clock knowledgeClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnProperty(
            name = "java-ai.knowledge.context-source",
            havingValue = "classpath",
            matchIfMissing = true
    )
    PolicyContextSource policyContextSource(
            @Value("classpath:knowledge/refund-policy-v1.properties") Resource metadata,
            @Value("classpath:knowledge/refund-policy-v1.md") Resource content
    ) {
        return new ClasspathPolicyContextSource(metadata, content);
    }

    @Bean
    TraceIdProvider traceIdProvider(ObjectProvider<Tracer> tracerProvider) {
        return new MicrometerTraceIdProvider(tracerProvider);
    }

    @Bean
    KnowledgeAnswerTelemetry knowledgeAnswerTelemetry(ObservationRegistry observationRegistry) {
        return new MicrometerKnowledgeAnswerTelemetry(observationRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
    KnowledgeAnswerModel disabledKnowledgeAnswerModel() {
        return new DisabledKnowledgeAnswerModel();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "none", matchIfMissing = true)
    KnowledgeAnswerStreamModel disabledKnowledgeAnswerStreamModel() {
        return new DisabledKnowledgeAnswerStreamModel();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
    KnowledgeAnswerModel springAiKnowledgeAnswerModel(
            ChatClient.Builder builder,
            @Value("${spring.ai.openai.chat.model}") String configuredModel
    ) {
        return new SpringAiKnowledgeAnswerModel(builder, configuredModel);
    }

    @Bean
    @ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
    KnowledgeAnswerStreamModel springAiKnowledgeAnswerStreamModel(
            ChatClient.Builder builder,
            @Value("${spring.ai.openai.chat.model}") String configuredModel
    ) {
        return new SpringAiKnowledgeAnswerStreamModel(builder, configuredModel);
    }

    @Bean
    AnswerKnowledgeQuestion answerKnowledgeQuestion(
            PolicyContextSource contextSource,
            KnowledgeAnswerModel answerModel,
            TraceIdProvider traceIdProvider,
            KnowledgeAnswerTelemetry telemetry,
            @Value("${java-ai.prompt.knowledge-answer.version:knowledge-answer-v1}") String promptVersion,
            @Value("classpath:prompts/knowledge-answer/v1/system.txt") Resource systemPrompt
    ) throws IOException {
        return new KnowledgeAnswerService(
                contextSource,
                answerModel,
                traceIdProvider,
                telemetry,
                promptVersion,
                systemPrompt.getContentAsString(StandardCharsets.UTF_8).strip()
        );
    }

    @Bean
    StreamKnowledgeAnswer streamKnowledgeAnswer(
            PolicyContextSource contextSource,
            KnowledgeAnswerStreamModel streamModel,
            TraceIdProvider traceIdProvider,
            @Value("${java-ai.prompt.knowledge-answer.version:knowledge-answer-v1}") String promptVersion,
            @Value("classpath:prompts/knowledge-answer/v1/system.txt") Resource systemPrompt
    ) throws IOException {
        return new StreamingKnowledgeAnswerService(
                contextSource,
                streamModel,
                traceIdProvider,
                promptVersion,
                systemPrompt.getContentAsString(StandardCharsets.UTF_8).strip()
        );
    }
}
