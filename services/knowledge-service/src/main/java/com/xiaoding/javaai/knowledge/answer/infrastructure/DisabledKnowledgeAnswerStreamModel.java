package com.xiaoding.javaai.knowledge.answer.infrastructure;

import com.xiaoding.javaai.knowledge.answer.application.GroundedPrompt;
import com.xiaoding.javaai.knowledge.answer.application.ModelNotConfiguredException;
import com.xiaoding.javaai.knowledge.answer.application.ModelStreamChunk;
import com.xiaoding.javaai.knowledge.answer.application.port.KnowledgeAnswerStreamModel;
import reactor.core.publisher.Flux;

final class DisabledKnowledgeAnswerStreamModel implements KnowledgeAnswerStreamModel {

    @Override
    public Flux<ModelStreamChunk> stream(GroundedPrompt prompt) {
        return Flux.error(new ModelNotConfiguredException());
    }
}
