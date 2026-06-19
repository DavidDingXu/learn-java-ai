package cn.dingxu.javaai.gateway;

import cn.dingxu.javaai.common.model.AiChatRequest;
import cn.dingxu.javaai.common.model.AiChatResponse;
import cn.dingxu.javaai.gateway.client.ModelClient;
import cn.dingxu.javaai.gateway.service.AiCallGateway;
import cn.dingxu.javaai.gateway.service.AiCallLogEntry;
import cn.dingxu.javaai.gateway.service.AiGatewayAdvisor;
import cn.dingxu.javaai.gateway.service.AiGatewayAdvisorChain;
import cn.dingxu.javaai.gateway.service.AiGatewayExchange;
import cn.dingxu.javaai.gateway.service.GatewayConversationMemoryAdvisor;
import cn.dingxu.javaai.gateway.service.InMemoryAiCallLogRepository;
import cn.dingxu.javaai.gateway.service.InMemoryGatewayConversationMemory;
import cn.dingxu.javaai.gateway.service.ModelRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AiGatewayAdvisorChainTest {

    @Test
    void advisorsEnrichPromptBeforeModelCallAndKeepExecutionEvents() {
        AtomicReference<String> promptSeenByModel = new AtomicReference<>();
        ModelClient fakeClient = new ModelClient() {
            @Override
            public String modelName() {
                return "fake-model";
            }

            @Override
            public String chat(String prompt) {
                promptSeenByModel.set(prompt);
                return "建议先核对历史沟通和订单状态。";
            }
        };
        AiGatewayAdvisor policyAdvisor = new AiGatewayAdvisor() {
            @Override
            public String name() {
                return "policy-context";
            }

            @Override
            public AiGatewayExchange advise(AiGatewayExchange exchange) {
                return exchange.appendPrompt("""

                        制度上下文：
                        大额退款必须先核对发货状态，并由人工确认。
                        """)
                        .addAdvisorEvent(name(), "APPEND_CONTEXT", "added refund policy context");
            }
        };

        InMemoryAiCallLogRepository logRepository = new InMemoryAiCallLogRepository();
        AiCallGateway gateway = new AiCallGateway(
                new ModelRouter(List.of(fakeClient)),
                logRepository,
                1,
                new AiGatewayAdvisorChain(List.of(policyAdvisor))
        );

        AiChatResponse response = gateway.chat(new AiChatRequest("u1001", "客户申请大额退款。"));

        assertThat(response.content()).contains("历史沟通");
        assertThat(promptSeenByModel.get()).contains("大额退款必须先核对发货状态");
        assertThat(logRepository.findAll())
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.status()).isEqualTo("SUCCESS");
                    assertThat(entry.advisorEvents()).containsExactly("policy-context:APPEND_CONTEXT");
                });
    }

    @Test
    void conversationMemoryAdvisorKeepsMemoryInGatewayBoundary() {
        InMemoryGatewayConversationMemory memory = new InMemoryGatewayConversationMemory();
        memory.append("u2001", "上一轮：客户说订单已经发货，但还没有签收。");
        memory.append("u2001", "上一轮：客服提醒需要先核对物流。");

        AtomicReference<String> promptSeenByModel = new AtomicReference<>();
        ModelClient fakeClient = new ModelClient() {
            @Override
            public String modelName() {
                return "fake-model";
            }

            @Override
            public String chat(String prompt) {
                promptSeenByModel.set(prompt);
                return "建议结合上一轮沟通核对物流状态。";
            }
        };

        InMemoryAiCallLogRepository logRepository = new InMemoryAiCallLogRepository();
        AiCallGateway gateway = new AiCallGateway(
                new ModelRouter(List.of(fakeClient)),
                logRepository,
                1,
                new AiGatewayAdvisorChain(List.of(new GatewayConversationMemoryAdvisor(memory, 2)))
        );

        gateway.chat(new AiChatRequest("u2001", "客户又追问退款进度。"));

        List<List<String>> advisorEvents = logRepository.findAll().stream()
                .map(AiCallLogEntry::advisorEvents)
                .toList();
        assertThat(promptSeenByModel.get()).contains("最近会话记忆");
        assertThat(promptSeenByModel.get()).contains("订单已经发货");
        assertThat(advisorEvents).containsExactly(List.of("conversation-memory:APPEND_MEMORY"));
    }
}
