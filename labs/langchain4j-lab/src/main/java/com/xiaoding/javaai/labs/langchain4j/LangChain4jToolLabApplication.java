package com.xiaoding.javaai.labs.langchain4j;

public final class LangChain4jToolLabApplication {

    private LangChain4jToolLabApplication() {
    }

    public static void main(String[] args) {
        LangChain4jLabApplication.runTool(
                LangChain4jLabApplication.createModel(LangChain4jLabApplication.loadConfig()));
    }
}
