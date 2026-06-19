package cn.dingxu.javaai.observability.service;

public enum SpanType {
    PROMPT,
    RAG,
    MODEL,
    TOOL,
    AGENT,
    STREAM,
    EVAL
}
