package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TenantScopedRagAdapterTest {

    @Test
    void passesTheTrustedScopeAndQueryIntoTheExistingSearchContract() {
        CapturingKnowledgeSearchPort search = new CapturingKnowledgeSearchPort();
        ContextAwareModel model = new ContextAwareModel();
        TenantScopedRagAdapter adapter = new TenantScopedRagAdapter(search, model, 4);
        KnowledgeAccessScope scope = new KnowledgeAccessScope("tenant-a", "user-7", List.of("finance"));

        PolicyAnswer answer = adapter.answer(scope, "报销多久到账？");

        assertEquals("tenant-a", search.scope.tenantId());
        assertEquals(List.of("finance"), search.scope.departments());
        assertEquals("报销多久到账？", search.query);
        assertEquals(4, search.topK);
        assertTrue(model.request.messages().stream().anyMatch(message -> message.toString().contains("FIN-2026-01")));
        assertEquals("依据 FIN-2026-01，审核通过后两个工作日内付款。", answer.text());
        assertEquals(List.of("FIN-2026-01"), answer.sourceIds());
    }

    private static final class CapturingKnowledgeSearchPort implements KnowledgeSearchPort {
        private KnowledgeAccessScope scope;
        private String query;
        private int topK;

        @Override
        public List<KnowledgeSnippet> search(KnowledgeAccessScope scope, String query, int topK) {
            this.scope = scope;
            this.query = query;
            this.topK = topK;
            return List.of(new KnowledgeSnippet("FIN-2026-01", "审核通过后两个工作日内付款。"));
        }
    }

    private static final class ContextAwareModel implements ChatModel {
        private ChatRequest request;

        @Override
        public ChatResponse doChat(ChatRequest request) {
            this.request = request;
            return ChatResponse.builder()
                    .aiMessage(new AiMessage("依据 FIN-2026-01，审核通过后两个工作日内付款。"))
                    .build();
        }
    }
}
