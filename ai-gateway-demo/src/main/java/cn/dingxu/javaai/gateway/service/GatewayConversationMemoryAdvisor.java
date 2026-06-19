package cn.dingxu.javaai.gateway.service;

import java.util.List;

public class GatewayConversationMemoryAdvisor implements AiGatewayAdvisor {

    private final GatewayConversationMemory memory;
    private final int maxMessages;

    public GatewayConversationMemoryAdvisor(GatewayConversationMemory memory, int maxMessages) {
        this.memory = memory;
        this.maxMessages = Math.max(0, maxMessages);
    }

    @Override
    public String name() {
        return "conversation-memory";
    }

    @Override
    public AiGatewayExchange advise(AiGatewayExchange exchange) {
        List<String> recentMessages = memory.recent(exchange.request().userId(), maxMessages);
        if (recentMessages.isEmpty()) {
            return exchange;
        }
        String prompt = "\n\n最近会话记忆：\n- " + String.join("\n- ", recentMessages) + "\n";
        return exchange.appendPrompt(prompt)
                .addAdvisorEvent(name(), "APPEND_MEMORY", "messages=" + recentMessages.size());
    }
}
