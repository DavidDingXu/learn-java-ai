# AI 安全回归验证

Status: VERIFIED_LOCAL_SECURITY_PIPELINE_SHARED_ENVIRONMENT_REQUIRED


## 已验证

- 版本化数据集包含 30 条用例，覆盖绕过确认、请求体污染身份和 Agent 审计详情中的模拟敏感片段。
- Agent Eval 检查禁止出现的执行事件和审计片段，且不会把敏感片段复制到报告中。
- 本地测试覆盖 Knowledge JWT、Ticket 调用方、Tool 目录、Prompt 分区和评测规则边界。
- 外部评测分别使用创建、运行和读取令牌，评测进程不具备确认权限。

## 本地验证

```bash
./mvnw -pl services/knowledge-service,services/ticket-agent-service,quality/eval-runner \
  -Dtest=KnowledgeJwtSecurityTest,TicketAgentJwtSecurityTest,BusinessToolCatalogTest,SpringAiTicketAgentPlannerPromptTest,AgentEvaluatorTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 外部验证边界

本地测试不扫描应用日志和 Trace，也没有运行双租户数据隔离场景。部署后的 `security-eval` 需要专用测试租户和已签名的短期令牌。这些结果验证安全门禁的结构，不代表已覆盖全部生产安全风险。
