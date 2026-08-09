package com.xiaoding.javaai.knowledge.answer.application;

import reactor.core.publisher.Mono;

public interface AnswerKnowledgeQuestion {

    Mono<KnowledgeAnswer> answer(AnswerKnowledgeQuestionCommand command);
}
