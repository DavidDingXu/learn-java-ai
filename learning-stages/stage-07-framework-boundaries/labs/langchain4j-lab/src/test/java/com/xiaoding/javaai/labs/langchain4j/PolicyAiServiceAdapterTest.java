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

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> port.answer(new PolicyQuestion("tenant-a", "退款多久到账？")));

        assertEquals("MODEL_OUTPUT_INVALID", exception.getMessage());
        assertTrue(exception.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void mapsModelFailuresToAStableApplicationError() {
        PolicyAnswerPort port = new LangChain4jPolicyAnswerAdapter(new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                throw new RuntimeException("provider-specific failure");
            }
        });

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> port.answer(new PolicyQuestion("tenant-a", "退款多久到账？")));

        assertEquals("MODEL_INVOCATION_FAILED", exception.getMessage());
        assertEquals("provider-specific failure", exception.getCause().getMessage());
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
