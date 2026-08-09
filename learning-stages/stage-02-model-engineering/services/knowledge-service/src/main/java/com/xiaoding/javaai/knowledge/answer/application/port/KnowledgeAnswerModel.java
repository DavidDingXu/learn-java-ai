package com.xiaoding.javaai.knowledge.answer.application.port;

import com.xiaoding.javaai.knowledge.answer.application.GroundedPrompt;
import com.xiaoding.javaai.knowledge.answer.application.ModelAnswerDraft;
import reactor.core.publisher.Mono;

public interface KnowledgeAnswerModel {

    Mono<ModelAnswerDraft> answer(GroundedPrompt prompt);
}
