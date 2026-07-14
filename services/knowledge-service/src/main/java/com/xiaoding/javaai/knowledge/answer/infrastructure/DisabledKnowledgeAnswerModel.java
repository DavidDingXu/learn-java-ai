package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.GroundedPrompt;
import com.xiaoding.javaai.knowledge.answer.application.ModelAnswerDraft;
import com.xiaoding.javaai.knowledge.answer.application.ModelNotConfiguredException;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerModel;
import reactor.core.publisher.Mono;

final class DisabledKnowledgeAnswerModel implements KnowledgeAnswerModel {

    @Override
    public Mono<ModelAnswerDraft> answer(GroundedPrompt prompt) {
        return Mono.error(new ModelNotConfiguredException());
    }
}
