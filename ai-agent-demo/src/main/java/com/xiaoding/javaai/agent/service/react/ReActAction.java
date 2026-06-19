package com.xiaoding.javaai.agent.service.react;

public record ReActAction(ReActActionType type, String name, String input) {

    public ReActAction {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        name = name == null ? "" : name;
        input = input == null ? "" : input;
    }

    public static ReActAction callTool(String toolName, String input) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must not be blank");
        }
        return new ReActAction(ReActActionType.CALL_TOOL, toolName, input);
    }

    public static ReActAction finish(String answer) {
        return new ReActAction(ReActActionType.FINISH, "finish", answer);
    }
}
