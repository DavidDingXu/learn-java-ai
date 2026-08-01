# 多 Agent 协作策略验证

Status: VERIFIED_DETERMINISTIC_POLICY


## 已验证

- Work-unit independence and side-effect facts select single-agent, multi-agent or human-required execution.
- Side effects produce a real AgentScope `RequireUserConfirmEvent`.
- Tests cover the single-agent, multi-agent and human-required branches and verify the AgentScope confirmation event type.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl agentscope-lab test
```

## 外部验证边界

The policy does not claim remote multi-agent execution. Production needs durable tasks, confirmation-to-task binding, delegated identity, budgets, timeout and result contracts.
