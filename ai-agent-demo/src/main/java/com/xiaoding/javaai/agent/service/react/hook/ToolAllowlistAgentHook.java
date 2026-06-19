package com.xiaoding.javaai.agent.service.react.hook;

import com.xiaoding.javaai.agent.service.react.ReActAction;
import com.xiaoding.javaai.agent.service.react.ReActActionType;
import com.xiaoding.javaai.agent.service.react.ReActStep;

import java.util.List;
import java.util.Set;

public class ToolAllowlistAgentHook implements AgentHook {

    private final Set<String> allowedTools;

    public ToolAllowlistAgentHook(Set<String> allowedTools) {
        this.allowedTools = allowedTools == null ? Set.of() : Set.copyOf(allowedTools);
    }

    @Override
    public String name() {
        return "tool-allowlist";
    }

    @Override
    public HookResult beforeAction(String userInput, List<ReActStep> previousSteps, ReActAction action) {
        if (action.type() != ReActActionType.CALL_TOOL || allowedTools.contains(action.name())) {
            return HookResult.accepted(action, List.of());
        }
        return HookResult.rejected(action, name(), List.of(name() + ":REJECT_TOOL"));
    }
}
