package com.xiaoding.javaai.ticket.task;

import com.xiaoding.javaai.ticket.security.DelegatedTicketIdentity;
import com.xiaoding.javaai.ticket.agent.domain.AgentTaskState;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.function.Supplier;

public final class AgentTaskIntakeService {

    private final Supplier<String> idGenerator;
    private final AgentTaskRepository repository;
    private final Clock clock;

    public AgentTaskIntakeService(
            Supplier<String> idGenerator,
            AgentTaskRepository repository,
            Clock clock
    ) {
        this.idGenerator = idGenerator;
        this.repository = repository;
        this.clock = clock;
    }

    public AgentTaskReceipt accept(
            DelegatedTicketIdentity identity,
            String idempotencyKey,
            AgentTaskRequest request
    ) {
        String key = requireKey(idempotencyKey);
        String fingerprint = fingerprint(request);
        AgentTaskRepository.TaskAcceptance acceptance = repository.accept(
                identity,
                key,
                fingerprint,
                () -> AgentTask.accepted(idGenerator.get(), identity, request, clock.instant()));
        return new AgentTaskReceipt(
                acceptance.task().taskId(), AgentTaskState.ACCEPTED.name(), acceptance.duplicate());
    }

    private static String fingerprint(AgentTaskRequest request) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, "caseId");
        append(canonical, request.caseId());
        append(canonical, "objective");
        append(canonical, request.objective());
        append(canonical, "businessContext");
        append(canonical, Integer.toString(request.businessContext().size()));
        request.businessContext().entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .forEach(entry -> {
                    append(canonical, entry.getKey());
                    append(canonical, entry.getValue());
                });
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    private static void append(StringBuilder canonical, String value) {
        canonical.append(value.length()).append(':').append(value).append(';');
    }

    private static String requireKey(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() < 8 || normalized.length() > 128) {
            throw new IllegalArgumentException("Idempotency-Key length must be between 8 and 128");
        }
        return normalized;
    }
}
