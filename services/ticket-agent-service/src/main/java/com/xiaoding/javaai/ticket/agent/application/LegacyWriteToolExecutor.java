package com.xiaoding.javaai.ticket.agent.application;

import com.xiaoding.javaai.ticket.agent.domain.ConfirmationRequest;
import com.xiaoding.javaai.ticket.agent.domain.ToolExecutionReceipt;
import com.xiaoding.javaai.ticket.task.AgentTask;

@FunctionalInterface
public interface LegacyWriteToolExecutor {
    ToolExecutionReceipt execute(
            AgentTask task,
            ConfirmationRequest confirmation,
            String idempotencyKey
    );
}
