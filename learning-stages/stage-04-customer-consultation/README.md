# 阶段 04：客户咨询

本阶段加入 Customer BFF、会话窗口、回答尝试、证据反馈、重试和工单接管。Ticket Agent Service 此时只接受工单任务，不包含 Planner、Tool 执行和人工确认编排。Knowledge Service 没有新增代码，根 `pom.xml` 直接复用阶段 03 的 RAG 实现。

填写 `config/application.yml` 后，在 IDEA 中依次运行：

- `KnowledgeServiceApplication`
- `TicketAgentServiceApplication`
- `CustomerBffApplication`

然后从 `POST http://localhost:8080/api/v1/customer/consultations/answers` 开始，按第 22-25 篇依次观察会话状态、引用、反馈、重试和接管回执。
