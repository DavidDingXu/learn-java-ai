package com.xiaoding.javaai.legacy.agent;

import com.xiaoding.javaai.legacy.agent.model.AgentTaskRequest;
import com.xiaoding.javaai.legacy.agent.model.AgentTaskResult;

public interface LegacyAgentClient {

    AgentTaskResult requestAdvice(AgentTaskRequest request);
}
