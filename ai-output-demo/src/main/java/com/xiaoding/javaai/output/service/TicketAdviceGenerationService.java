package com.xiaoding.javaai.output.service;

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
        if (!realModelAvailable(builder)) {
            TicketAdviceResponse advice = buildSampleAdvice(input);
            return new GenerationOutput("sample", prompt, null, advice);
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

    private boolean realModelAvailable(ChatClient.Builder builder) {
        if (builder == null) {
            return false;
        }
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }
        return !apiKey.equals("demo-key") && !apiKey.equals("replace-with-your-api-key");
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

    private TicketAdviceResponse buildSampleAdvice(TicketAdviceGenerationInput input) {
        return new TicketAdviceResponse(
                sampleSummary(input.ticket()),
                RiskLevel.MEDIUM,
                java.util.List.of("核对物流状态", "转人工复核"),
                java.util.List.of("refund-policy-001")
        );
    }

    private String rawContent(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        return response.getResult().getOutput().getText();
    }

    private String sampleSummary(String ticket) {
        if (ticket == null || ticket.isBlank()) {
            return "客户申请退款但订单已经发货";
        }
        return ticket.replace("\"", "'");
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
