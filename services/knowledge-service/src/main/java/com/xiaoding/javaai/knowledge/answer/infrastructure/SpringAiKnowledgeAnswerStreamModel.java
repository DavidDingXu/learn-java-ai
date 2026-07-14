package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.GroundedPrompt;
import com.xiaoding.javaai.knowledge.answer.application.ModelStreamChunk;
import com.xiaoding.javaai.knowledge.answer.application.ModelUsage;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerStreamModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;

import java.util.Locale;

final class SpringAiKnowledgeAnswerStreamModel implements KnowledgeAnswerStreamModel {

    private final ChatClient chatClient;
    private final String configuredModel;

    SpringAiKnowledgeAnswerStreamModel(ChatClient.Builder builder, String configuredModel) {
        this.chatClient = builder.build();
        this.configuredModel = configuredModel;
    }

    @Override
    public Flux<ModelStreamChunk> stream(GroundedPrompt prompt) {
        return chatClient.prompt()
                .system(prompt.systemInstruction())
                .user(streamingUserMessage(prompt))
                .stream()
                .chatResponse()
                .map(this::toChunk);
    }

    private ModelStreamChunk toChunk(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage providerUsage = metadata == null ? null : metadata.getUsage();
        ModelUsage usage = providerUsage == null ? null : new ModelUsage(
                valueOrZero(providerUsage.getPromptTokens()),
                valueOrZero(providerUsage.getCompletionTokens()),
                valueOrZero(providerUsage.getTotalTokens())
        );
        String model = metadata == null || metadata.getModel() == null
                ? configuredModel : metadata.getModel();
        String finishReason = response.getResult() == null
                ? null : normalizeFinishReason(response.getResult().getMetadata().getFinishReason());
        String delta = response.getResult() == null || response.getResult().getOutput() == null
                ? null : response.getResult().getOutput().getText();
        return new ModelStreamChunk(delta, model, usage, finishReason);
    }

    private static String streamingUserMessage(GroundedPrompt prompt) {
        return SpringAiKnowledgeAnswerModel.buildUserMessage(
                prompt,
                "请直接输出给客户看的纯文本答案，不要输出 JSON、Markdown 或代码块。"
        );
    }

    private static String normalizeFinishReason(String finishReason) {
        return finishReason == null ? null : finishReason.toLowerCase(Locale.ROOT);
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
