package com.xiaoding.javaai.knowledge.answer.application.port;

import com.xiaoding.javaai.knowledge.answer.application.GroundedPrompt;
import com.xiaoding.javaai.knowledge.answer.application.ModelStreamChunk;
import reactor.core.publisher.Flux;

public interface KnowledgeAnswerStreamModel {

    Flux<ModelStreamChunk> stream(GroundedPrompt prompt);
}
