# Lesson 25 Ticket Handoff Evidence

Status: VERIFIED_LOCAL_IDEMPOTENT_HANDOFF

Implementation commit: `2cbe5398e6cfec9090bed091947f6b0d261077ee`

## Verified

- 工单升级只能引用已完成回答尝试，并生成包含问题、回答、引用、拒答、反馈、摘要、原因码和源 Trace 的不可变快照。
- 幂等键由 tenant、conversation 和 attempt 经过带字段边界的 SHA-256 计算，格式固定为 `handoff:v1:{digest}`。网络超时后重试不会换键，也不会在 Header 中暴露原始业务标识。
- BFF 调 Ticket Agent 时只发送业务快照；tenant、customer、角色和部门继续来自委托 JWT，不进入请求体。
- Ticket Agent 校验 `ticket-agent-service` audience、`customer-bff` actor 与 `ticket:task:create` scope。
- Ticket Agent 对请求内容生成 SHA-256 指纹：同键同内容返回原 task 且标记 duplicate，同键异内容返回 409。
- `customer-bff-v1.yaml`、`knowledge-service-v1.yaml` 和 `agent-task-v1.yaml` 已进入统一接口定义解析器。

## Local Verification

```bash
./mvnw -pl apps/customer-bff,services/ticket-agent-service \
  -Dtest=WebClientTicketTaskClientTest,AgentTaskIntakeServiceTest,AgentTaskControllerTest,TicketAgentServiceApplicationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

./mvnw -pl quality/eval-runner -Dtest=ContractValidatorTest test package
java -jar quality/eval-runner/target/eval-runner-0.1.0-SNAPSHOT-all.jar contract-validate contracts
```

接口定义解析结果：4 份 OpenAPI、2 份 JSON Schema、2 个正向夹具和 2 个负向夹具全部通过。

## Production Boundary

Ticket Agent 的运行配置使用 PostgreSQL/Flyway 持久化任务、请求指纹和状态，进程内实现只保留在测试配置中。公司环境必须继续验证目标 PostgreSQL 的事务隔离、并发领取、备份恢复与容量；后续 Agent、Tool 和 JDK8 回调只能在同一任务基础上继续，不能重新解释一份无来源文本。
