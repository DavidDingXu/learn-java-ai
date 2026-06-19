package cn.dingxu.javaai.mcp;

public record McpResourceContent(boolean visible, String uri, String content) {
    public McpResourceContent {
        uri = uri == null ? "" : uri;
        content = content == null ? "" : content;
    }

    public static McpResourceContent hidden(String uri) {
        return new McpResourceContent(false, uri, "");
    }

    public static McpResourceContent visible(String uri, String content) {
        return new McpResourceContent(true, uri, content);
    }
}
