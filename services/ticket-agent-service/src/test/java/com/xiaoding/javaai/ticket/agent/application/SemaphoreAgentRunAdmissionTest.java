package com.xiaoding.javaai.ticket.agent.application;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SemaphoreAgentRunAdmissionTest {

    @Test
    void rejects_excess_concurrency_and_releases_the_permit_after_completion() throws Exception {
        SemaphoreAgentRunAdmission admission = new SemaphoreAgentRunAdmission(1);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var running = executor.submit(() -> admission.execute(() -> {
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("test release timed out");
                    }
                } catch (InterruptedException error) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(error);
                }
                return "done";
            }));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> admission.execute(() -> "second"))
                    .isInstanceOf(AgentCapacityExceededException.class);

            release.countDown();
            assertThat(running.get(5, TimeUnit.SECONDS)).isEqualTo("done");
            assertThat(admission.execute(() -> "next")).isEqualTo("next");
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void releases_the_permit_after_a_failure() {
        SemaphoreAgentRunAdmission admission = new SemaphoreAgentRunAdmission(1);

        assertThatThrownBy(() -> admission.execute(() -> {
            throw new IllegalStateException("planner failed");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(admission.execute(() -> "recovered")).isEqualTo("recovered");
    }
}
