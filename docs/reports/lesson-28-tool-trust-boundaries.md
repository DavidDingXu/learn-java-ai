# Tool 输入与结果边界验证

Status: VERIFIED_SERVER_OWNED_TOOL_POLICY


## 已验证

- 用户目标、业务上下文和 Tool 输出分别放入不同的不可信 Prompt 区域。
- 模型提案不能提供租户、操作者、角色、风险、端点、令牌或幂等策略。
- 服务端规范化允许的参数，并生成确定性的 SHA-256 Tool 调用指纹。
- 结构化输出解析后，队列编码、退款金额和币种仍由确定性的 Java 规则校验。
- Tool 输出始终只是数据；每个后续动作仍要重新经过同一份目录策略和状态机。

## 验证命令

```bash
./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,HttpLegacyWriteToolExecutorTest test
```

## 生产接入边界

Prompt partitioning is not a standalone injection defense. Company rollout must add security datasets for user input, business context and tool-output injection, and must keep authorization in downstream services.
