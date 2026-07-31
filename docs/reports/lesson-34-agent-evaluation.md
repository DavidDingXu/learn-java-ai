# 第 34 篇：Agent 评测验证记录

Status: VERIFIED_AGENT_EVALUATION_PIPELINE

## 已验证的行为

- 30 条版本化路径样例记录目标、业务上下文、期望状态、工具、参数、风险、角色和禁止出现的审计事件。
- Eval Runner 通过公开 HTTP 创建任务、运行任务并读取审计；创建、运行和读取分别使用最小权限令牌，Runner 没有确认权限。
- 评测器逐条报告字段差异和客户端错误，不用一个平均分掩盖具体失败。
- JSON 与 Markdown 报告关联数据集版本、代码版本、单条结果和延迟。
- 路径集禁止确认前出现 Tool 执行事件；确定性业务测试另外覆盖未知工具、非法参数、过期或重复确认和幂等冲突。

## 验证命令

```bash
./mvnw -pl quality/eval-runner \
  -Dtest=AgentEvalDatasetLoaderTest,AgentEvaluatorTest,AgentTaskHttpEvaluationClientTest,AgentEvaluationReportWriterTest test

./mvnw -pl services/ticket-agent-service \
  -Dtest=BusinessToolCatalogTest,TicketAgentOrchestratorTest,ToolConfirmationServiceTest,AgentTaskIntakeServiceTest test
```

## 适用范围

这套路径集验证确认前的工具选择、参数、风险和副作用边界，不代表生产准确率。公司门禁还需要加入脱敏的高风险、拒绝、注入和授权负向样例，并在没有生产写权限的专用租户运行。
