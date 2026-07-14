package com.xiaoding.javaai.labs.langchain4j;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

import java.util.Objects;

public final class LangChain4jPolicyAnswerAdapter implements PolicyAnswerPort {

    private final PolicyAssistant assistant;

    public LangChain4jPolicyAnswerAdapter(ChatModel chatModel) {
        this.assistant = AiServices.builder(PolicyAssistant.class)
                .chatModel(Objects.requireNonNull(chatModel, "chatModel must not be null"))
                .build();
    }

    @Override
    public PolicyAnswer answer(PolicyQuestion question) {
        return new PolicyAnswer(assistant.answer(question.tenantId(), question.question()));
    }

    interface PolicyAssistant {
        @SystemMessage("你是企业制度助手。只处理当前租户的问题；没有可靠依据时明确拒答。")
        @UserMessage("租户：{{tenantId}}\n问题：{{question}}")
        String answer(@V("tenantId") String tenantId, @V("question") String question);
    }
}
