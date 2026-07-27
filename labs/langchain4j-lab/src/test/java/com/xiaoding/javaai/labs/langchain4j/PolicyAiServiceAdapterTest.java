package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolicyAiServiceAdapterTest {

    @Test
    void migratesOneBusinessUseCaseBehindAStablePort() {
        CapturingModel model = new CapturingModel("退款将在三个工作日内原路退回。");
        PolicyAnswerPort port = new LangChain4jPolicyAnswerAdapter(model);

        PolicyAnswer answer = port.answer(new PolicyQuestion("tenant-a", "退款多久到账？"));

        assertEquals("退款将在三个工作日内原路退回。", answer.text());
        assertTrue(model.request.messages().stream().anyMatch(message -> message.toString().contains("tenant-a")));
        assertTrue(model.request.messages().stream().anyMatch(message -> message.toString().contains("退款多久到账")));
    }

    @Test
    void rejectsBlankModelAnswersAtTheBusinessBoundary() {
        PolicyAnswerPort port = new LangChain4jPolicyAnswerAdapter(new CapturingModel("  "));

        assertThrows(IllegalArgumentException.class,
                () -> port.answer(new PolicyQuestion("tenant-a", "退款多久到账？")));
    }

    private static final class CapturingModel implements ChatModel {
        private final String answer;
        private ChatRequest request;

        private CapturingModel(String answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            this.request = request;
            return ChatResponse.builder().aiMessage(new AiMessage(answer)).modelName("fixture").build();
        }
    }
}
