package cn.dingxu.javaai.agent.service.react.hook;

import cn.dingxu.javaai.agent.service.react.ReActAction;
import cn.dingxu.javaai.agent.service.react.ReActStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AgentHookChain {

    private final List<AgentHook> hooks;

    public AgentHookChain(List<AgentHook> hooks) {
        this.hooks = hooks == null ? List.of() : hooks.stream()
                .sorted(Comparator.comparing(AgentHook::name))
                .toList();
    }

    public HookResult beforeAction(String userInput, List<ReActStep> previousSteps, ReActAction action) {
        ReActAction current = action;
        List<String> events = new ArrayList<>();
        for (AgentHook hook : hooks) {
            HookResult result = hook.beforeAction(userInput, previousSteps, current);
            events.addAll(result.events());
            current = result.action();
            if (result.rejected()) {
                return HookResult.rejected(current, result.reason(), events);
            }
        }
        return HookResult.accepted(current, events);
    }
}
