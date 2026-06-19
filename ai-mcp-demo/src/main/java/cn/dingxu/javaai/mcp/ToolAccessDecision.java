package cn.dingxu.javaai.mcp;

public record ToolAccessDecision(boolean allowed, String reason) {

    public static ToolAccessDecision allow() {
        return new ToolAccessDecision(true, "");
    }

    public static ToolAccessDecision deny(String reason) {
        return new ToolAccessDecision(false, reason == null ? "" : reason);
    }
}
