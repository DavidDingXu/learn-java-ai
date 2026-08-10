package com.xiaoding.javaai.labs.agentscope;

public interface CollaborationAgent extends AutoCloseable {

    String name();

    String call(String request);

    @Override
    default void close() {
    }
}
