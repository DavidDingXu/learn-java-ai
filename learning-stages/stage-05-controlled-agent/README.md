# 阶段 05：受控 Agent

本阶段加入真实 `TicketAgentOrchestrator`、Spring AI Planner、只读与写 Tool、风险分级、人工确认、幂等执行、审计和 JDK 8 调用边界。Knowledge Service 与 Customer BFF 没有改动，根 `pom.xml` 分别复用阶段 03 和阶段 04 的模块；本目录只保留 Agent、Agent Eval 与 JDK 8 增量。

填写项目根目录唯一的 `config/application-default.yml` 后，在 IDEA 中运行 `KnowledgeServiceApplication`、`TicketAgentServiceApplication`、`CustomerBffApplication`。随后打开 `agent-learning-journey.http`，按第 26-34 篇创建任务、运行任务、确认高风险动作并读取审计时间线。

JDK 8 边界使用独立的 `integrations/jdk8-client`，入口是 `TicketAgentClient`。它只依赖稳定 HTTP 契约，不依赖 Spring AI。

| 篇目 | 对应入口 | 先看代码 | 读者会看到 |
|---|---|---|---|
| 26 | HTTP `02`、`03` | `TicketAgentOrchestrator` | 有限步 Agent 任务开始运行 |
| 27 | 运行只读路径 | `BusinessToolCatalog`、`HttpKnowledgeReadToolExecutor` | 模型只能选择服务端注册的 Tool |
| 28 | 查看任务与审计 | `PreparedToolCall`、`AgentAuditTrail` | Tool 参数、权限和结果可追踪 |
| 29 | 触发写动作 | `ToolRisk`、`ToolEffect` | 风险由服务端事实判定 |
| 30 | HTTP `06-confirm-write-action` | `ToolConfirmationService` | 从任务响应自动取得确认单和版本，再明确批准 |
| 31 | HTTP `07-repeat-the-same-confirmation` | `ToolActionFingerprint`、`ConfirmationDecisionStore` | 同一决定返回原 actionId，并标记为重复 |
| 32 | `TicketAgentClient` | JDK 8 HTTP DTO | 老系统只依赖稳定协议，不引入模型框架 |
| 33 | 从 HTTP `02` 到 `08` | `TicketAgentOrchestrator` | 同一任务完成知识查询、确认、写 Tool 和最终审计 |
| 34 | `EvalRunner` 的 `agent-eval` | Agent Golden Set | 本地固定身份无需准备令牌即可生成评测报告 |
