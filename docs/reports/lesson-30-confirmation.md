# 人工确认验证

Status: VERIFIED_VERSION_BOUND_IDEMPOTENT_CONFIRMATION


## 已验证

- 确认单绑定确认 ID、动作 ID、Tool、风险、角色、规范化参数、指纹、任务版本和过期时间。
- 确认人来自可信 JWT Claim，且必须满足租户、`jdk8-crm` 操作者和所需角色约束。
- 过期任务版本、失效确认单、错误租户、错误操作者和缺失角色都会在 Tool 执行前被拒绝。
- 确认决定按租户、操作者和业务主体划分幂等范围，并比较请求指纹。
- 完全相同的重复决定返回原回执；相同幂等键对应不同内容时返回冲突。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=ToolConfirmationServiceTest,ConfirmationActorFactoryTest,AgentTaskWorkflowControllerTest test
```

## 生产接入边界

Confirmation and idempotency state are in-memory only in tests. The runtime configuration uses JDBC/Flyway, durable unique constraints and optimistic task updates; an expired pending write is not automatically reclaimed without downstream reconciliation. High-risk domains may still require multi-party approval rather than a single confirmation.
