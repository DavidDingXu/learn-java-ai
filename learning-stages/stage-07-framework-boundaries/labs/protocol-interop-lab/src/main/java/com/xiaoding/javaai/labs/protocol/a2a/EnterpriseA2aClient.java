package com.xiaoding.javaai.labs.protocol.a2a;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.TaskEvent;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfig;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TransportProtocol;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class EnterpriseA2aClient implements AutoCloseable {

    private final String baseUrl;
    private final Set<String> allowedSkills;
    private AgentCard card;
    private Client client;

    public EnterpriseA2aClient(String baseUrl, Set<String> allowedSkills) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("baseUrl must not be blank");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.allowedSkills = Set.copyOf(allowedSkills);
    }

    public A2aDiscoveryReceipt discover() {
        card = A2A.getAgentCard(baseUrl);
        AgentInterface jsonRpc = card.supportedInterfaces().stream()
                .filter(candidate -> TransportProtocol.JSONRPC.asString()
                        .equalsIgnoreCase(candidate.protocolBinding()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("agent does not expose JSON-RPC"));

        List<String> discovered = card.skills().stream()
                .map(skill -> skill.id())
                .filter(allowedSkills::contains)
                .toList();
        if (discovered.size() != allowedSkills.size()) {
            Set<String> missing = new java.util.LinkedHashSet<>(allowedSkills);
            missing.removeAll(discovered);
            throw new IllegalStateException("allowlisted A2A skills were not discovered: " + missing);
        }
        client = Client.builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                .build();
        return new A2aDiscoveryReceipt(
                card.name(), card.version(), jsonRpc.protocolVersion(), discovered);
    }

    public Task send(String objective) {
        if (client == null) discover();
        AtomicReference<Task> task = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        client.sendMessage(
                A2A.toUserMessage(objective),
                List.of((event, ignored) -> {
                    if (event instanceof TaskEvent taskEvent) task.set(taskEvent.getTask());
                }),
                failure::set,
                null);
        if (failure.get() != null) {
            throw new IllegalStateException("A2A request failed", failure.get());
        }
        if (task.get() == null) {
            throw new IllegalStateException("A2A response did not contain a task");
        }
        return task.get();
    }

    @Override
    public void close() {
        if (client != null) client.close();
    }
}
