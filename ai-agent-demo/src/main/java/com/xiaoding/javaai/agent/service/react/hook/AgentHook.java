package com.xiaoding.javaai.agent.service.react.hook;

import com.xiaoding.javaai.agent.service.react.ReActAction;
import com.xiaoding.javaai.agent.service.react.ReActStep;

import java.util.List;

public interface AgentHook {

    String name();

    HookResult beforeAction(String userInput, List<ReActStep> previousSteps, ReActAction action);
}
