package cn.dingxu.javaai.agent.service.react;

import java.util.List;

public record ReActStep(int index, String actionName, String actionInput, String observation, List<String> hookEvents) {

    public ReActStep {
        if (index < 1) {
            throw new IllegalArgumentException("index must be greater than 0");
        }
        actionName = actionName == null ? "" : actionName;
        actionInput = actionInput == null ? "" : actionInput;
        observation = observation == null ? "" : observation;
        hookEvents = hookEvents == null ? List.of() : List.copyOf(hookEvents);
    }

    public ReActStep(int index, String actionName, String actionInput, String observation) {
        this(index, actionName, actionInput, observation, List.of());
    }
}
