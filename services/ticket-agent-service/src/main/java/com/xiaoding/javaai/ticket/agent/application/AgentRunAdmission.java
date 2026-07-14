package com.xiaoding.javaai.ticket.agent.application;

import java.util.function.Supplier;

public interface AgentRunAdmission {

    AgentRunAdmission UNLIMITED = Supplier::get;

    <T> T execute(Supplier<T> operation);
}
