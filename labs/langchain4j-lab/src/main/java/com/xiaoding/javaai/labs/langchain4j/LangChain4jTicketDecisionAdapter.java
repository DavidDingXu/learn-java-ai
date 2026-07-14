package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Objects;

public final class LangChain4jTicketDecisionAdapter {

    private final TicketDecisionAssistant assistant;

    public LangChain4jTicketDecisionAdapter(ChatModel chatModel, TicketReadTools tools) {
        this.assistant = AiServices.builder(TicketDecisionAssistant.class)
                .chatModel(Objects.requireNonNull(chatModel, "chatModel must not be null"))
                .tools(Objects.requireNonNull(tools, "tools must not be null"))
                .build();
    }

    public TicketDecision decide(String ticketId, String instruction) {
        TicketDecision decision = assistant.decide(ticketId, instruction);
        if (!ticketId.equals(decision.ticketId())) {
            throw new IllegalStateException("model returned a decision for another ticket");
        }
        return decision;
    }

    interface TicketDecisionAssistant {
        @SystemMessage("你负责工单分流。先调用 query_ticket 获取事实，再输出结构化决定。不得执行写操作。")
        @UserMessage("工单：{{ticketId}}\n任务：{{instruction}}")
        TicketDecision decide(@V("ticketId") String ticketId, @V("instruction") String instruction);
    }
}
