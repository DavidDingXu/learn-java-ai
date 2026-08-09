package com.xiaoding.javaai.ticket.agent.application;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

public final class SemaphoreAgentRunAdmission implements AgentRunAdmission {

    private final Semaphore permits;

    public SemaphoreAgentRunAdmission(int maxConcurrentRuns) {
        if (maxConcurrentRuns < 1) {
            throw new IllegalArgumentException("maxConcurrentRuns must be positive");
        }
        this.permits = new Semaphore(maxConcurrentRuns, true);
    }

    @Override
    public <T> T execute(Supplier<T> operation) {
        if (!permits.tryAcquire()) {
            throw new AgentCapacityExceededException();
        }
        try {
            return operation.get();
        } finally {
            permits.release();
        }
    }
}
