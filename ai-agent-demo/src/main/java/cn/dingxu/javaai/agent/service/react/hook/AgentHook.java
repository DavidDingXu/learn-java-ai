package cn.dingxu.javaai.agent.service.react.hook;

import cn.dingxu.javaai.agent.service.react.ReActAction;
import cn.dingxu.javaai.agent.service.react.ReActStep;

import java.util.List;

public interface AgentHook {

    String name();

    HookResult beforeAction(String userInput, List<ReActStep> previousSteps, ReActAction action);
}
