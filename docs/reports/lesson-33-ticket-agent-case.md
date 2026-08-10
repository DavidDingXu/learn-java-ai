# 工单 Agent 业务链验证

Status: VERIFIED_COMPONENT_CHAIN_SHARED_ENVIRONMENT_REQUIRED


## 已验证

- 转人工任务会依次经过有限步规划、只读 Tool 观察和写 Tool 确认边界。
- 真实 Spring AI 2.0 模型在冒烟验证中选择了 `QUERY_KNOWLEDGE`，并返回结构化元数据。
- Knowledge 与旧系统 HTTP 适配器分别约束目标 audience、scope、超时和响应映射。
- 委托 JWT 统一使用 `departmentIds`，Knowledge 与 Ticket 服务都会校验允许的操作者。
- 公开的 Agent Task OpenAPI 支持创建、读取、运行、确认和审计操作。
- 模型、只读 Tool、确认和远程执行失败分别对应不同的任务状态与 HTTP 行为。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service,services/knowledge-service \
  -Dtest=TicketAgentOrchestratorTest,HttpKnowledgeReadToolExecutorTest,HttpLegacyWriteToolExecutorTest,TicketAgentJwtSecurityTest,KnowledgeJwtSecurityTest,AgentTaskWorkflowControllerTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

./mvnw -pl services/ticket-agent-service \
  -Dtest=TicketAgentLiveModelSmokeIT \
  -Dspring.config.additional-location=file:../../config/application-default.yml \
  -Djava-ai.agent-smoke.report-path=target/agent-live-model-smoke.md \
  test
```

## 外部验证边界

真实模型 Smoke 只验证 Planner 的在线调用，不代表已部署完整生产链路。真实 IdP、持久任务仓储、已准备的 Knowledge 索引和 JDK8 业务沙箱，仍需要在共享环境使用已签名的短期令牌验证。
