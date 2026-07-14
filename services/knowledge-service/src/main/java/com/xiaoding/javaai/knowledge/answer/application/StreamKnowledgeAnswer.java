package com.xiaoding.javaai.knowledge.answer.application;

import reactor.core.publisher.Flux;

public interface StreamKnowledgeAnswer {

    Flux<AnswerStreamEvent> stream(AnswerKnowledgeQuestionCommand command);
}
