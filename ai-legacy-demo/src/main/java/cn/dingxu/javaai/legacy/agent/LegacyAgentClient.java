package cn.dingxu.javaai.legacy.agent;

import cn.dingxu.javaai.legacy.agent.model.AgentTaskRequest;
import cn.dingxu.javaai.legacy.agent.model.AgentTaskResult;

public interface LegacyAgentClient {

    AgentTaskResult requestAdvice(AgentTaskRequest request);
}
