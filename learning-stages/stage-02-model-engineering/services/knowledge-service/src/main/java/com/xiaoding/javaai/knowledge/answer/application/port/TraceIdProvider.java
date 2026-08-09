package com.xiaoding.javaai.knowledge.answer.application.port;

@FunctionalInterface
public interface TraceIdProvider {

    String currentTraceId();
}
