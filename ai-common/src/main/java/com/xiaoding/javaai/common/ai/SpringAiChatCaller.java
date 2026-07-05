package com.xiaoding.javaai.common.ai;

import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

public class SpringAiChatCaller {

    private final ChatClient.Builder builder;
    private final String apiKey;
    private final String modelName;
    private final String feature;

    public SpringAiChatCaller(ChatClient.Builder builder,
                              String apiKey,
                              String modelName,
                              String feature) {
        this.builder = builder;
        this.apiKey = apiKey;
        this.modelName = modelName == null || modelName.isBlank() ? "unknown-model" : modelName;
        this.feature = feature == null || feature.isBlank() ? "live AI call" : feature;
    }

    public LiveAiResult call(String systemPrompt, String userPrompt) {
        ChatClient.ChatClientRequestSpec request = request(systemPrompt, userPrompt);
        String content = request.call().content();
        return new LiveAiResult("model:" + modelName, modelName, content);
    }

    public Flux<String> stream(String systemPrompt, String userPrompt) {
        return request(systemPrompt, userPrompt).stream().content();
    }

    public ChatClient.ChatClientRequestSpec request(String systemPrompt, String userPrompt) {
        RealAiRuntime.requireConfigured(apiKey, feature);
        if (builder == null) {
            throw new IllegalStateException(feature + " requires a Spring AI ChatClient.Builder bean");
        }
        return builder.build()
                .prompt()
                .system(systemPrompt == null ? "" : systemPrompt)
                .user(userPrompt == null ? "" : userPrompt);
    }
}
