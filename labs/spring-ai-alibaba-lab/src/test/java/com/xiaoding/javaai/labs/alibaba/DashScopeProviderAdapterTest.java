package com.xiaoding.javaai.labs.alibaba;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DashScopeProviderAdapterTest {

    @Test
    void mapsBusinessPromptToDashScopeOptionsWithoutLeakingProviderTypes() {
        CapturingChatModel model = new CapturingChatModel();
        DashScopeProviderAdapter adapter = new DashScopeProviderAdapter(model, "qwen-plus", 0.2, 512);

        ProviderAnswer answer = adapter.answer("你只能依据给定制度回答。", "退款多久到账？");

        assertEquals("退款将在三个工作日内原路退回。", answer.text());
        assertEquals("qwen-plus", answer.model());
        assertFalse(answer.providerMetadata().containsKey("apiKey"));
        DashScopeChatOptions options = (DashScopeChatOptions) model.prompt.getOptions();
        assertEquals("qwen-plus", options.getModel());
        assertEquals(0.2, options.getTemperature());
        assertEquals(512, options.getMaxTokens());
        assertEquals(false, options.getEnableSearch());
        assertEquals("你只能依据给定制度回答。", model.prompt.getSystemMessage().getText());
        assertEquals("退款多久到账？", model.prompt.getUserMessage().getText());
    }

    private static final class CapturingChatModel implements ChatModel {
        private Prompt prompt;

        @Override
        public ChatResponse call(Prompt prompt) {
            this.prompt = prompt;
            return new ChatResponse(List.of(new Generation(new AssistantMessage("退款将在三个工作日内原路退回。"))));
        }
    }
}
