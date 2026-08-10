package com.xiaoding.javaai.labs.langchain4j;

public final class LangChain4jRagLabApplication {

    private LangChain4jRagLabApplication() {
    }

    public static void main(String[] args) {
        LangChain4jLabApplication.runRag(
                LangChain4jLabApplication.createModel(LangChain4jLabApplication.loadConfig()));
    }
}
