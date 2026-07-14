# AgentScope Lab

This lab uses AgentScope Java 2.0 for runtime and protocol boundaries while retaining server-side business control.

- `AgentScopeTicketRuntime` maps trusted identity and Tool policy into `PermissionEngine` decisions.
- `CollaborationPolicy` selects single-agent, multi-agent or human-required execution from dependency and side-effect facts.
- `EnterpriseMcpRegistry` imports only HTTPS, allowlisted, read-only remote schemas as external AgentScope tools.
- `A2aTaskCoordinator` owns idempotency, request fingerprints, monotonic status and UNKNOWN delivery outcomes.

```bash
../../mvnw -f ../pom.xml -pl agentscope-lab test
```
