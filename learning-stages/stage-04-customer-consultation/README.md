# 阶段 04：客户咨询

本阶段加入 Customer Web、Customer BFF、会话窗口、回答尝试、证据反馈、重试和工单接管。Ticket Agent Service 此时只接受工单任务，不包含 Planner、Tool 执行和人工确认编排。Knowledge Service 没有新增代码，根 `pom.xml` 直接复用阶段 03 的 RAG 实现。

填写项目根目录唯一的 `config/application-default.yml` 后，在 IDEA 中依次运行：

- `KnowledgeServiceApplication`
- `TicketAgentServiceApplication`
- `CustomerBffApplication`

然后打开 `consultation-learning-journey.http`，按第 22-25 篇依次观察会话状态、引用、反馈、重试和接管回执。

要观察真实页面，在 IDEA 中打开 `apps/customer-web/package.json`，安装依赖后运行 `dev`。访问 `http://127.0.0.1:5173`，本地固定身份下令牌输入框可以留空。

| 篇目 | 对应操作 | 先看代码 | 读者会看到 |
|---|---|---|---|
| 22 | 页面提问或 HTTP `02-answer-and-open-session` | `CustomerConsultationController`、`customer-bff-client.ts` | 页面流式显示回答与引用，BFF 创建会话 |
| 23 | 继续同一会话 | `ConsultationSession` | 有边界的短期上下文，不把全部历史塞给模型 |
| 24 | HTTP `03`、`04` | `RecordAnswerFeedback` | 引用、负反馈和重试成为业务状态 |
| 25 | HTTP `05-handoff-to-ticket` | `HandoffConsultation`、`WebClientTicketTaskClient` | AI 无法解决时返回可追踪的工单回执 |
