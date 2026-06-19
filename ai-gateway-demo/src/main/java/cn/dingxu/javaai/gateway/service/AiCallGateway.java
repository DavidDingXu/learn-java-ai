package cn.dingxu.javaai.gateway.service;

import cn.dingxu.javaai.common.model.AiChatRequest;
import cn.dingxu.javaai.common.model.AiChatResponse;
import cn.dingxu.javaai.common.trace.AiTrace;
import cn.dingxu.javaai.gateway.client.ModelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AiCallGateway {

    private final ModelRouter modelRouter;
    private final AiCallLogRepository logRepository;
    private final int maxAttempts;
    private final Duration callTimeout;
    private final ExecutorService executor;
    private final AiGatewayAdvisorChain advisorChain;

    public AiCallGateway(ModelRouter modelRouter,
                         AiCallLogRepository logRepository,
                         AiGatewayAdvisorChain advisorChain,
                         @Value("${java-ai.gateway.max-attempts:2}") int maxAttempts,
                         @Value("${java-ai.gateway.call-timeout:30s}") Duration callTimeout) {
        this(modelRouter, logRepository, maxAttempts, callTimeout, Executors.newCachedThreadPool(), advisorChain);
    }

    public AiCallGateway(ModelRouter modelRouter,
                         AiCallLogRepository logRepository,
                         int maxAttempts) {
        this(modelRouter, logRepository, maxAttempts, Duration.ofSeconds(30), Executors.newCachedThreadPool(), new AiGatewayAdvisorChain(List.of()));
    }

    public AiCallGateway(ModelRouter modelRouter,
                         AiCallLogRepository logRepository,
                         int maxAttempts,
                         AiGatewayAdvisorChain advisorChain) {
        this(modelRouter, logRepository, maxAttempts, Duration.ofSeconds(30), Executors.newCachedThreadPool(), advisorChain);
    }

    public AiCallGateway(ModelRouter modelRouter,
                         AiCallLogRepository logRepository,
                         int maxAttempts,
                         Duration callTimeout,
                         ExecutorService executor) {
        this(modelRouter, logRepository, maxAttempts, callTimeout, executor, new AiGatewayAdvisorChain(List.of()));
    }

    public AiCallGateway(ModelRouter modelRouter,
                         AiCallLogRepository logRepository,
                         int maxAttempts,
                         Duration callTimeout,
                         ExecutorService executor,
                         AiGatewayAdvisorChain advisorChain) {
        this.modelRouter = modelRouter;
        this.logRepository = logRepository;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.callTimeout = callTimeout == null ? Duration.ofSeconds(30) : callTimeout;
        this.executor = executor;
        this.advisorChain = advisorChain == null ? new AiGatewayAdvisorChain(List.of()) : advisorChain;
    }

    public AiChatResponse chat(AiChatRequest request) {
        AiTrace trace = AiTrace.start(request.userId(), "gateway.chat");
        AiGatewayExchange exchange = advisorChain.apply(AiGatewayExchange.start(trace, request, buildPrompt(request)));
        RuntimeException lastError = null;

        for (ModelClient client : modelRouter.candidates()) {
            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                long startedAt = System.nanoTime();
                try {
                    String content = callWithTimeout(client, exchange.prompt());
                    long latencyMs = elapsedMs(startedAt);
                    logRepository.save(AiCallLogEntry.success(
                            trace.traceId(),
                            request.userId(),
                            trace.scenario(),
                            client.modelName(),
                            attempt,
                            latencyMs,
                            exchange.advisorEvents()
                    ));
                    return new AiChatResponse(trace.traceId(), client.modelName(), content, latencyMs);
                } catch (RuntimeException error) {
                    long latencyMs = elapsedMs(startedAt);
                    lastError = error;
                    logRepository.save(AiCallLogEntry.failed(
                            trace.traceId(),
                            request.userId(),
                            trace.scenario(),
                            client.modelName(),
                            attempt,
                            latencyMs,
                            error,
                            exchange.advisorEvents()
                    ));
                }
            }
        }

        throw new IllegalStateException("all model clients failed", lastError);
    }

    private String callWithTimeout(ModelClient client, String prompt) {
        Future<String> future = executor.submit(() -> client.chat(prompt));
        try {
            return future.get(callTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException error) {
            future.cancel(true);
            throw new IllegalStateException("model call timed out after " + callTimeout.toMillis() + "ms", error);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("model call interrupted", error);
        } catch (ExecutionException error) {
            Throwable cause = error.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("model call failed", cause);
        }
    }

    private String buildPrompt(AiChatRequest request) {
        return """
                你是企业工单系统里的 AI 辅助助手。
                请根据用户输入输出简洁、可执行的处理建议。

                用户输入：
                %s
                """.formatted(request.message());
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
