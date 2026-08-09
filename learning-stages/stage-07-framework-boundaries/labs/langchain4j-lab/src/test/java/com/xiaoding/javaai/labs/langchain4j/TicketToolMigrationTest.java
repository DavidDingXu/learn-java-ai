package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TicketToolMigrationTest {

    @Test
    void executesAReadToolThenParsesStructuredBusinessOutput() {
        ScriptedToolModel model = new ScriptedToolModel();
        TicketReadTools tools = new TicketReadTools(ticketId -> new TicketSnapshot(ticketId, "OPEN", "等待退款"));
        LangChain4jTicketDecisionAdapter adapter = new LangChain4jTicketDecisionAdapter(model, tools);

        TicketDecision decision = adapter.decide("T-1001", "请判断是否需要人工处理");

        assertEquals("T-1001", decision.ticketId());
        assertEquals(TicketDecisionType.ESCALATE, decision.decision());
        assertEquals(1, tools.invocationCount());
        assertEquals(2, model.calls.get());
    }

    @Test
    void rejectsAStructuredDecisionForAnotherTicket() {
        ScriptedToolModel model = new ScriptedToolModel("T-OTHER");
        TicketReadTools tools = new TicketReadTools(
                ticketId -> new TicketSnapshot(ticketId, "OPEN", "等待退款"));
        LangChain4jTicketDecisionAdapter adapter = new LangChain4jTicketDecisionAdapter(model, tools);

        assertThrows(IllegalStateException.class,
                () -> adapter.decide("T-1001", "请判断是否需要人工处理"));
        assertEquals(1, tools.invocationCount());
        assertEquals(2, model.calls.get());
    }

    private static final class ScriptedToolModel implements ChatModel {
        private final AtomicInteger calls = new AtomicInteger();
        private final String decisionTicketId;

        private ScriptedToolModel() {
            this("T-1001");
        }

        private ScriptedToolModel(String decisionTicketId) {
            this.decisionTicketId = decisionTicketId;
        }

        @Override
        public ChatResponse doChat(ChatRequest request) {
            if (calls.incrementAndGet() == 1) {
                ToolExecutionRequest toolCall = ToolExecutionRequest.builder()
                        .id("call-1")
                        .name("query_ticket")
                        .arguments("{\"ticket_id\":\"T-1001\"}")
                        .build();
                return ChatResponse.builder().aiMessage(new AiMessage(List.of(toolCall))).build();
            }
            return ChatResponse.builder()
                    .aiMessage(new AiMessage(("""
                            {"ticketId":"%s","decision":"ESCALATE","reason":"退款等待需要人工核验"}
                            """).formatted(decisionTicketId)))
                    .build();
        }
    }
}
