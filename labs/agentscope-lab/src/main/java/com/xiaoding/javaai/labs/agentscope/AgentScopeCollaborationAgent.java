package com.xiaoding.javaai.labs.agentscope;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.tool.Toolkit;

public final class AgentScopeCollaborationAgent implements CollaborationAgent {

    private final String name;
    private final ReActAgent agent;
    private final RuntimeContext context;

    private AgentScopeCollaborationAgent(
            String name,
            ReActAgent agent,
            RuntimeContext context) {
        this.name = name;
        this.agent = agent;
        this.context = context;
    }

    public static AgentScopeCollaborationAgent specialist(
            String name,
            String systemPrompt,
            Model model,
            Toolkit toolkit,
            PermissionContextState permissions) {
        ReActAgent agent = ReActAgent.builder()
                .name(name)
                .sysPrompt(systemPrompt)
                .model(model)
                .toolkit(toolkit)
                .permissionContext(permissions)
                .maxIters(4)
                .build();
        return new AgentScopeCollaborationAgent(name, agent, context(name));
    }

    public static AgentScopeCollaborationAgent synthesizer(
            String name,
            String systemPrompt,
            Model model) {
        ReActAgent agent = ReActAgent.builder()
                .name(name)
                .sysPrompt(systemPrompt)
                .model(model)
                .maxIters(2)
                .build();
        return new AgentScopeCollaborationAgent(name, agent, context(name));
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String call(String request) {
        Msg result = agent.call(request, context).block();
        if (result == null || result.getTextContent() == null || result.getTextContent().isBlank()) {
            throw new IllegalStateException(name + " returned no answer");
        }
        return result.getTextContent();
    }

    @Override
    public void close() {
        agent.close();
    }

    private static RuntimeContext context(String name) {
        return RuntimeContext.builder()
                .sessionId("tenant-a:" + name + ":T-100")
                .userId("employee-7")
                .build();
    }
}
