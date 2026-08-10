package com.xiaoding.javaai.labs.agentscope;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class MultiAgentCoordinator {

    private final Duration specialistTimeout;

    public MultiAgentCoordinator(Duration specialistTimeout) {
        if (specialistTimeout == null || specialistTimeout.isZero() || specialistTimeout.isNegative()) {
            throw new IllegalArgumentException("specialistTimeout must be positive");
        }
        this.specialistTimeout = specialistTimeout;
    }

    public MultiAgentResult execute(
            String request,
            List<CollaborationAgent> specialists,
            CollaborationAgent synthesizer) {
        if (request == null || request.isBlank()) {
            throw new IllegalArgumentException("request must not be blank");
        }
        if (specialists == null || specialists.size() < 2) {
            throw new IllegalArgumentException("multi-agent execution requires at least two specialists");
        }
        if (synthesizer == null) {
            throw new IllegalArgumentException("synthesizer must not be null");
        }
        Set<String> names = new HashSet<>();
        if (specialists.stream().anyMatch(agent -> !names.add(agent.name()))) {
            throw new IllegalArgumentException("specialist names must be unique");
        }

        List<SpecialistResult> results;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Callable<SpecialistResult>> tasks = specialists.stream()
                    .<Callable<SpecialistResult>>map(agent -> () -> call(agent, request))
                    .toList();
            List<Future<SpecialistResult>> futures;
            try {
                futures = executor.invokeAll(tasks, specialistTimeout.toNanos(), TimeUnit.NANOSECONDS);
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                return new MultiAgentResult(
                        MultiAgentStatus.HUMAN_REQUIRED, List.of(), null,
                        "specialist execution interrupted");
            }
            results = new ArrayList<>(futures.size());
            for (int index = 0; index < futures.size(); index++) {
                results.add(result(futures.get(index), specialists.get(index).name()));
            }
        }

        List<SpecialistResult> failures = results.stream()
                .filter(result -> !result.succeeded())
                .toList();
        if (!failures.isEmpty()) {
            String reason = failures.stream()
                    .map(failure -> failure.agentName() + ": " + failure.error())
                    .collect(Collectors.joining("; "));
            return new MultiAgentResult(
                    MultiAgentStatus.HUMAN_REQUIRED, results, null,
                    "required specialist failed; " + reason);
        }

        String evidence = results.stream()
                .map(result -> result.agentName() + ": " + result.answer())
                .collect(Collectors.joining("\n"));
        try {
            String answer = synthesizer.call(
                    "请只根据以下专家结果回答原始问题。\n原始问题：" + request + "\n专家结果：\n" + evidence);
            return new MultiAgentResult(MultiAgentStatus.COMPLETED, results, answer, null);
        } catch (RuntimeException error) {
            return new MultiAgentResult(
                    MultiAgentStatus.HUMAN_REQUIRED, results, null,
                    "synthesizer failed: " + message(error));
        }
    }

    private static SpecialistResult call(CollaborationAgent agent, String request) {
        try {
            return SpecialistResult.success(agent.name(), agent.call(request));
        } catch (RuntimeException error) {
            return SpecialistResult.failure(agent.name(), message(error));
        }
    }

    private SpecialistResult result(Future<SpecialistResult> future, String agentName) {
        if (future.isCancelled()) {
            return SpecialistResult.failure(
                    agentName, "timed out after " + specialistTimeout.toMillis() + "ms");
        }
        try {
            return future.get();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return SpecialistResult.failure(agentName, "execution interrupted");
        } catch (ExecutionException error) {
            return SpecialistResult.failure(agentName, message(error.getCause()));
        }
    }

    private static String message(Throwable error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? error.getClass().getSimpleName()
                : error.getMessage();
    }
}
