package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    SpringAiKnowledgeAnswerStreamModel(ChatClient.Builder builder, String configuredModel) {
        this.chatClient = builder.build();
        this.configuredModel = configuredModel;
    }

    @Override
    public Flux<ModelStreamChunk> stream(GroundedPrompt prompt) {
        return Flux.defer(() -> {
            StreamingAnswerHeaderParser parser = new StreamingAnswerHeaderParser(objectMapper);
            return chatClient.prompt()
                    .system(prompt.systemInstruction())
                    .user(streamingUserMessage(prompt))
                    .stream()
                    .chatResponse()
                    .map(this::toChunk)
                    .map(parser::parse);
        });
    }

    private ModelStreamChunk toChunk(ChatResponse response) {
        ChatResponseMetadata metadata = response.getMetadata();
        Usage providerUsage = metadata == null ? null : metadata.getUsage();
        ModelUsage usage = mapUsage(providerUsage);
        String model = metadata == null || metadata.getModel() == null
                ? configuredModel : metadata.getModel();
        String finishReason = response.getResult() == null
                ? null : normalizeFinishReason(response.getResult().getMetadata().getFinishReason());
        String delta = response.getResult() == null || response.getResult().getOutput() == null
                ? null : response.getResult().getOutput().getText();
        return new ModelStreamChunk(delta, model, usage, finishReason);
    }

    static String streamingUserMessage(GroundedPrompt prompt) {
        return SpringAiKnowledgeAnswerModel.buildUserMessage(prompt) + "\n\n" + """
                输出必须严格以这一段决策头开头，标签前不要添加任何文字：
                <answer-decision>{"citedSectionIds":["本次上下文中的 sectionId"],"refused":false,"refusalReason":null}</answer-decision><answer-text>
                citedSectionIds 只能选择 AUTHORIZED_KNOWLEDGE_CONTEXT 中存在的 sectionId。
                证据不足时将 refused 设为 true、citedSectionIds 设为空数组，并提供具体的 refusalReason。
                <answer-text> 后只输出给客户看的纯文本答案，不要输出 Markdown、代码块或结束标签。
                """;
    }

    private static ModelUsage mapUsage(Usage usage) {
        if (usage == null) return null;
        int promptTokens = valueOrZero(usage.getPromptTokens());
        int completionTokens = valueOrZero(usage.getCompletionTokens());
        int totalTokens = valueOrZero(usage.getTotalTokens());
        if (promptTokens == 0 && completionTokens == 0 && totalTokens == 0) return null;
        return new ModelUsage(promptTokens, completionTokens, totalTokens);
    }

    private static String normalizeFinishReason(String finishReason) {
        return finishReason == null ? null : finishReason.toLowerCase(Locale.ROOT);
    }

    private static int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
