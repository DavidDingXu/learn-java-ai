package cn.dingxu.javaai.mcp;

public record McpPromptMessage(String system, String user) {
    public McpPromptMessage {
        system = system == null ? "" : system;
        user = user == null ? "" : user;
    }
}
