package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;

import java.util.List;
import java.util.Objects;

public final class TenantScopedRagAdapter {

    private final KnowledgeSearchPort searchPort;
    private final ChatModel chatModel;
    private final int topK;

    public TenantScopedRagAdapter(KnowledgeSearchPort searchPort, ChatModel chatModel, int topK) {
        this.searchPort = Objects.requireNonNull(searchPort, "searchPort must not be null");
        this.chatModel = Objects.requireNonNull(chatModel, "chatModel must not be null");
        if (topK <= 0) {
            throw new IllegalArgumentException("topK must be positive");
        }
        this.topK = topK;
    }

    public PolicyAnswer answer(KnowledgeAccessScope scope, String question) {
        List<KnowledgeSnippet> snippets = searchPort.search(scope, question, topK);
        ContentRetriever retriever = query -> snippets.stream()
                .map(snippet -> Content.from("来源 " + snippet.sourceId() + "\n" + snippet.text()))
                .toList();
        KnowledgeAssistant assistant = AiServices.builder(KnowledgeAssistant.class)
                .chatModel(chatModel)
                .contentRetriever(retriever)
                .build();
        String answer = assistant.answer(question);
        return new PolicyAnswer(answer, snippets.stream().map(KnowledgeSnippet::sourceId).toList());
    }

    interface KnowledgeAssistant {
        @SystemMessage("只依据检索内容回答，并在答案中保留来源编号。依据不足时拒答。")
        String answer(String question);
    }
}
