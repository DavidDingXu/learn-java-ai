package com.xiaoding.javaai.agent;

import com.xiaoding.javaai.agent.service.context.ContextSlice;
import com.xiaoding.javaai.agent.service.context.ContextSliceType;
import com.xiaoding.javaai.agent.service.context.EngineeredContext;
import com.xiaoding.javaai.agent.service.memory.AgentContext;
import com.xiaoding.javaai.agent.service.springai.SpringAiAdvisorMemoryBridge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiAdvisorMemoryBridgeTest {

    private final SpringAiAdvisorMemoryBridge bridge = new SpringAiAdvisorMemoryBridge();

    @Test
    void bindsSpringAiConversationIdToTenantSessionAndBusinessObject() {
        var binding = bridge.bind(context(), engineeredContext());

        assertThat(binding.conversationId()).isEqualTo("tenant-a:session-1:T-1001");
        assertThat(binding.advisorParams())
                .containsEntry(ChatMemory.CONVERSATION_ID, "tenant-a:session-1:T-1001")
                .containsEntry("java-ai.context.totalEstimatedTokens", 24);
        assertThat(binding.sources()).containsExactly("businessSnapshot", "conversation");
    }

    @Test
    void usesSpringAiMessageMemoryAdvisorWithoutGatewayAdapter() {
        var binding = bridge.bind(context(), engineeredContext());
        ChatMemory memory = bridge.windowMemory(4);
        memory.add(binding.conversationId(), new UserMessage("上一轮：订单已经发货，但客户还没有签收。"));

        var advisor = bridge.memoryAdvisor(memory);
        ChatClientRequest request = ChatClientRequest.builder()
                .prompt(new Prompt(new UserMessage("客户追问退款进度。")))
                .context(binding.advisorParams())
                .build();

        ChatClientRequest processed = advisor.before(request, new AdvisorChain() {
        });

        assertThat(processed.prompt().getInstructions())
                .extracting(Message::getText)
                .containsExactly("上一轮：订单已经发货，但客户还没有签收。", "客户追问退款进度。");
        assertThat(memory.get(binding.conversationId()))
                .extracting(Message::getText)
                .contains("客户追问退款进度。");
    }

    private AgentContext context() {
        return new AgentContext(
                "tenant-a",
                "session-1",
                "T-1001",
                "u1001",
                List.of(),
                Optional.empty(),
                List.of(),
                List.of("conversation")
        );
    }

    private EngineeredContext engineeredContext() {
        return new EngineeredContext(
                List.of(new ContextSlice(
                        ContextSliceType.BUSINESS_SNAPSHOT,
                        "businessSnapshot",
                        "业务快照: orderStatus=SHIPPED",
                        24,
                        Map.of("businessId", "T-1001")
                )),
                24,
                List.of("businessSnapshot", "conversation")
        );
    }
}
