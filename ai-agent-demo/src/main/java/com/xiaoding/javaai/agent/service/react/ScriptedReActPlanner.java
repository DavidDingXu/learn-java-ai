package com.xiaoding.javaai.agent.service.react;

import java.util.List;

public class ScriptedReActPlanner implements ReActPlanner {

    private final List<ReActAction> actions;

    public ScriptedReActPlanner(List<ReActAction> actions) {
        this.actions = actions == null ? List.of() : List.copyOf(actions);
    }

    @Override
    public ReActAction next(String userInput, List<ReActStep> previousSteps) {
        int nextIndex = previousSteps == null ? 0 : previousSteps.size();
        if (nextIndex >= actions.size()) {
            return ReActAction.finish("没有更多可执行步骤，转人工处理。");
        }
        return actions.get(nextIndex);
    }
}
