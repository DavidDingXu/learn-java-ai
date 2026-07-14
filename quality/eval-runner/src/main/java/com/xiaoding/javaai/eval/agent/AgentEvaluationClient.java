package com.xiaoding.javaai.eval.agent;

import java.net.URI;

@FunctionalInterface
public interface AgentEvaluationClient {
    AgentEvaluationSnapshot evaluate(
            URI baseUrl,
            AgentEvaluationTokens tokens,
            AgentEvalCase evalCase,
            String idempotencyKey
    );
}
