package com.xiaoding.javaai.output;

import com.xiaoding.javaai.output.service.TicketAdviceGenerationService;
import com.xiaoding.javaai.output.service.TicketAdviceGenerationService.TicketAdviceGenerationInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.beans.factory.ObjectProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TicketAdviceGenerationServiceTest {

    @Test
    void refusesToGenerateAdviceWithoutRealAiConfiguration() {
        TicketAdviceGenerationService service = new TicketAdviceGenerationService(
                objectProvider(null),
                "demo-key",
                "gpt-4o-mini"
        );

        assertThatThrownBy(() -> service.generate(new TicketAdviceGenerationInput(
                "客户申请退款但订单已经发货",
                "已发货订单需要先核对物流状态"
        )))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ai-output-demo")
                .hasMessageContaining("AI_API_KEY");
    }

    @Test
    @SuppressWarnings("unchecked")
    void modelModeUsesSpringAiStructuredOutputConverter() {
        ChatClient.Builder builder = mock(ChatClient.Builder.class);
        ChatClient chatClient = mock(ChatClient.class);
        ChatClient.ChatClientRequestSpec requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        ChatClient.CallResponseSpec responseSpec = mock(ChatClient.CallResponseSpec.class);
        TicketAdviceResponse advice = new TicketAdviceResponse(
                "客户申请退款但订单已经发货",
                RiskLevel.MEDIUM,
                java.util.List.of("核对物流状态"),
                java.util.List.of("refund-policy-001")
        );

        when(builder.build()).thenReturn(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(org.mockito.ArgumentMatchers.anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.responseEntity(anyStructuredOutputConverter()))
                .thenReturn(new ResponseEntity<ChatResponse, TicketAdviceResponse>(null, advice));

        TicketAdviceGenerationService service = new TicketAdviceGenerationService(
                objectProvider(builder),
                "real-key",
                "gpt-4o-mini"
        );

        var output = service.generate(new TicketAdviceGenerationInput(
                "客户申请退款但订单已经发货",
                "已发货订单需要先核对物流状态"
        ));

        assertThat(output.mode()).isEqualTo("model:gpt-4o-mini");
        assertThat(output.advice()).isSameAs(advice);
        assertThat(output.prompt()).contains("Your response should be in JSON format");
        var converterCaptor = forClass(StructuredOutputConverter.class);
        verify(responseSpec).responseEntity(converterCaptor.capture());
        assertThat(converterCaptor.getValue().getFormat()).contains("riskLevel");
        assertThat(converterCaptor.getValue().getFormat()).contains("nextActions");
    }

    private ObjectProvider<ChatClient.Builder> objectProvider(ChatClient.Builder builder) {
        return new TestObjectProvider(builder);
    }

    private static final class TestObjectProvider implements ObjectProvider<ChatClient.Builder> {
        private final ChatClient.Builder builder;

        private TestObjectProvider(ChatClient.Builder builder) {
            this.builder = builder;
        }

            @Override
            public ChatClient.Builder getObject(Object... args) throws BeansException {
                return builder;
            }

            @Override
            public ChatClient.Builder getIfAvailable() throws BeansException {
                return builder;
            }

            @Override
            public ChatClient.Builder getIfUnique() throws BeansException {
                return builder;
            }

            @Override
            public ChatClient.Builder getObject() throws BeansException {
                return builder;
            }
    }

    @SuppressWarnings("unchecked")
    private StructuredOutputConverter<TicketAdviceResponse> anyStructuredOutputConverter() {
        return any(StructuredOutputConverter.class);
    }
}
