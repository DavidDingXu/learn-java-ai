package com.xiaoding.javaai.gateway.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SpringAiModelClient implements ModelClient {

    private final ChatClient chatClient;
    private final String modelName;

    public SpringAiModelClient(ChatClient.Builder builder,
                               @Value("${java-ai.gateway.model-name}") String modelName) {
        this.chatClient = builder.build();
        this.modelName = modelName;
    }

    @Override
    public String modelName() {
        return modelName;
    }

    @Override
    public String chat(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
