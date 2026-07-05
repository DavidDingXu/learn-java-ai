package com.xiaoding.javaai.output.service;

import com.xiaoding.javaai.common.ai.RealAiRuntime;
import com.xiaoding.javaai.output.RiskLevel;
import com.xiaoding.javaai.output.TicketAdviceResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TicketAdviceGenerationService {

    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String apiKey;
    private final String modelName;

    public TicketAdviceGenerationService(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                         @Value("${spring.ai.openai.api-key:}") String apiKey,
                                         @Value("${java-ai.output.model-name:gpt-4o-mini}") String modelName) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.apiKey = apiKey;
        this.modelName = modelName;
    }

    public GenerationOutput generate(TicketAdviceGenerationInput input) {
        BeanOutputConverter<TicketAdviceResponse> outputConverter = new BeanOutputConverter<>(TicketAdviceResponse.class);
        String prompt = buildPrompt(input, outputConverter);
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        RealAiRuntime.requireConfigured(apiKey, "ai-output-demo");
        if (builder == null) {
            throw new IllegalStateException("ai-output-demo requires a Spring AI ChatClient.Builder bean");
        }

        try {
            var response = builder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .responseEntity(outputConverter);
            TicketAdviceResponse advice = response.entity();
            if (advice == null) {
                throw new IllegalArgumentException("model response did not contain ticket advice");
            }
            return new GenerationOutput("model:" + modelName, prompt, rawContent(response.response()), advice);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("model response cannot become TicketAdviceResponse", e);
        }
    }

    private String buildPrompt(TicketAdviceGenerationInput input,
                               BeanOutputConverter<TicketAdviceResponse> outputConverter) {
        return new PromptTemplate("""
                你是企业工单系统里的 AI 助手。请根据工单和制度生成工单处理建议。
                工单：{ticket}
                制度：{policy}
                {format}
                """).render(Map.of(
                "ticket", input.ticket(),
                "policy", input.policy(),
                "format", outputConverter.getFormat()
        ));
    }

    private String rawContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    public record TicketAdviceGenerationInput(String ticket, String policy) {
        public TicketAdviceGenerationInput {
            if (ticket == null || ticket.isBlank()) {
                throw new IllegalArgumentException("ticket must not be blank");
            }
            if (policy == null || policy.isBlank()) {
                throw new IllegalArgumentException("policy must not be blank");
            }
        }
    }

    public record GenerationOutput(String mode, String prompt, String rawOutput, TicketAdviceResponse advice) {
    }
}
