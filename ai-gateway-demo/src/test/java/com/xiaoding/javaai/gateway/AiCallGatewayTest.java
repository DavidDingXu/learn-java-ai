package com.xiaoding.javaai.gateway;

import com.xiaoding.javaai.common.model.AiChatRequest;
import com.xiaoding.javaai.common.model.AiChatResponse;
import com.xiaoding.javaai.gateway.client.ModelClient;
import com.xiaoding.javaai.gateway.service.AiCallLogEntry;
import com.xiaoding.javaai.gateway.service.AiCallGateway;
import com.xiaoding.javaai.gateway.service.InMemoryAiCallLogRepository;
import com.xiaoding.javaai.gateway.service.ModelRouter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class AiCallGatewayTest {

    @Test
    void wrapsModelResponseWithTraceAndLatency() {
        ModelClient fakeClient = new ModelClient() {
            @Override
            public String modelName() {
                return "fake-helpdesk-model";
            }

            @Override
            public String chat(String prompt) {
                assertThat(prompt).contains("客户申请退款");
                return "建议先核对订单发货状态，再按退款制度处理。";
            }
        };

        InMemoryAiCallLogRepository logRepository = new InMemoryAiCallLogRepository();
        AiCallGateway gateway = new AiCallGateway(
                new ModelRouter(List.of(fakeClient)),
                logRepository,
                1
        );
        AiChatResponse response = gateway.chat(new AiChatRequest("u1001", "客户申请退款，但订单已经发货。"));

        assertThat(response.traceId()).isNotBlank();
        assertThat(response.model()).isEqualTo("fake-helpdesk-model");
        assertThat(response.content()).contains("退款制度");
        assertThat(response.latencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(logRepository.findAll())
                .singleElement()
                .extracting(AiCallLogEntry::status)
                .isEqualTo("SUCCESS");
    }

    @Test
    void retriesPrimaryModelBeforeReturningSuccess() {
        AtomicInteger attempts = new AtomicInteger();
        ModelClient flakyPrimary = new ModelClient() {
            @Override
            public String modelName() {
                return "primary-model";
            }

            @Override
            public String chat(String prompt) {
                if (attempts.incrementAndGet() == 1) {
                    throw new IllegalStateException("temporary model error");
                }
                return "第二次调用成功，建议先查订单。";
            }
        };

        InMemoryAiCallLogRepository logRepository = new InMemoryAiCallLogRepository();
        AiCallGateway gateway = new AiCallGateway(
                new ModelRouter(List.of(flakyPrimary)),
                logRepository,
                2
        );

        AiChatResponse response = gateway.chat(new AiChatRequest("u1001", "客户申请退款"));

        assertThat(response.model()).isEqualTo("primary-model");
        assertThat(response.content()).contains("查订单");
        assertThat(attempts).hasValue(2);
        assertThat(logRepository.findAll())
                .extracting(AiCallLogEntry::status)
                .containsExactly("FAILED", "SUCCESS");
    }

    @Test
    void fallsBackToBackupModelWhenPrimaryKeepsFailing() {
        ModelClient failingPrimary = new ModelClient() {
            @Override
            public String modelName() {
                return "primary-model";
            }

            @Override
            public String chat(String prompt) {
                throw new IllegalStateException("primary down");
            }
        };
        ModelClient backup = new ModelClient() {
            @Override
            public String modelName() {
                return "backup-model";
            }

            @Override
            public String chat(String prompt) {
                return "备用模型返回：先转人工确认退款风险。";
            }
        };

        InMemoryAiCallLogRepository logRepository = new InMemoryAiCallLogRepository();
        AiCallGateway gateway = new AiCallGateway(
                new ModelRouter(List.of(failingPrimary, backup)),
                logRepository,
                2
        );

        AiChatResponse response = gateway.chat(new AiChatRequest("u1001", "客户申请退款"));

        assertThat(response.model()).isEqualTo("backup-model");
        assertThat(response.content()).contains("转人工");
        assertThat(logRepository.findAll())
                .extracting(AiCallLogEntry::model, AiCallLogEntry::attempt, AiCallLogEntry::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("primary-model", 1, "FAILED"),
                        org.assertj.core.groups.Tuple.tuple("primary-model", 2, "FAILED"),
                        org.assertj.core.groups.Tuple.tuple("backup-model", 1, "SUCCESS")
                );
    }

    @Test
    void fallsBackToBackupModelWhenPrimaryTimesOut() {
        ModelClient slowPrimary = new ModelClient() {
            @Override
            public String modelName() {
                return "slow-primary-model";
            }

            @Override
            public String chat(String prompt) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("model call interrupted", interrupted);
                }
                return "这个结果太晚了，不应该被网关采用。";
            }
        };
        ModelClient backup = new ModelClient() {
            @Override
            public String modelName() {
                return "fast-backup-model";
            }

            @Override
            public String chat(String prompt) {
                return "备用模型及时返回：建议先核对订单状态。";
            }
        };

        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            InMemoryAiCallLogRepository logRepository = new InMemoryAiCallLogRepository();
            AiCallGateway gateway = new AiCallGateway(
                    new ModelRouter(List.of(slowPrimary, backup)),
                    logRepository,
                    1,
                    Duration.ofMillis(30),
                    executor
            );

            AiChatResponse response = gateway.chat(new AiChatRequest("u1001", "客户申请退款"));

            assertThat(response.model()).isEqualTo("fast-backup-model");
            assertThat(response.content()).contains("核对订单");
            assertThat(logRepository.findAll())
                    .extracting(AiCallLogEntry::model, AiCallLogEntry::status)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("slow-primary-model", "FAILED"),
                            org.assertj.core.groups.Tuple.tuple("fast-backup-model", "SUCCESS")
                    );
            assertThat(logRepository.findAll().getFirst().errorMessage()).contains("timed out");
        } finally {
            executor.shutdownNow();
        }
    }
}
