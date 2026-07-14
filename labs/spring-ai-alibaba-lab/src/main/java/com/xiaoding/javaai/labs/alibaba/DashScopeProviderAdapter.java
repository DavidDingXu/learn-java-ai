package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DashScopeProviderAdapter {

    private final ChatModel chatModel;
    private final String modelName;
    private final double temperature;
    private final int maxTokens;

    public DashScopeProviderAdapter(ChatModel chatModel, String modelName, double temperature, int maxTokens) {
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        this.modelName = requireText(modelName, "modelName");
        if (temperature < 0 || temperature >= 2) {
            throw new IllegalArgumentException("temperature must be in [0, 2)");
        }
        if (maxTokens <= 0) {
            throw new IllegalArgumentException("maxTokens must be positive");
        }
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    public ProviderAnswer answer(String systemInstruction, String question) {
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model(modelName)
                .temperature(temperature)
                .maxToken(maxTokens)
                .enableSearch(false)
                .build();
        Prompt prompt = new Prompt(
                List.of(new SystemMessage(requireText(systemInstruction, "systemInstruction")),
                        new UserMessage(requireText(question, "question"))),
                options);
        ChatResponse response = Objects.requireNonNull(chatModel.call(prompt), "chat response must not be null");
        Generation generation = Objects.requireNonNull(response.getResult(), "chat response must contain a result");

        Map<String, Object> metadata = new LinkedHashMap<>();
        if (response.getMetadata() != null && response.getMetadata().getId() != null) {
            metadata.put("responseId", response.getMetadata().getId());
        }
        String responseModel = response.getMetadata() == null ? null : response.getMetadata().getModel();
        if (responseModel != null && !responseModel.isBlank()) {
            metadata.put("responseModel", responseModel);
        }
        String finishReason = generation.getMetadata().getFinishReason();
        if (finishReason != null && !finishReason.isBlank()) {
            metadata.put("finishReason", finishReason);
        }
        return new ProviderAnswer(generation.getOutput().getText(),
                responseModel == null || responseModel.isBlank() ? modelName : responseModel,
                metadata);
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
