package com.xiaoding.javaai.knowledge.indexing.infrastructure;

import com.xiaoding.javaai.knowledge.indexing.application.IndexTaskWorker;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(IndexTaskWorker.class)
@ConditionalOnProperty(
        name = "java-ai.knowledge.indexing.scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public final class IndexTaskScheduler {

    private final IndexTaskWorker worker;

    public IndexTaskScheduler(IndexTaskWorker worker) {
        this.worker = worker;
    }

    @Scheduled(
            initialDelayString = "${java-ai.knowledge.indexing.scheduler-initial-delay:5s}",
            fixedDelayString = "${java-ai.knowledge.indexing.scheduler-delay:1s}"
    )
    void poll() {
        worker.runOnce();
    }
}
