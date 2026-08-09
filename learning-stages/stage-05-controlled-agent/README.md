# 阶段 05：受控 Agent

本阶段加入真实 `TicketAgentOrchestrator`、Spring AI Planner、只读与写 Tool、风险分级、人工确认、幂等执行、审计和 JDK 8 调用边界。Knowledge Service 与 Customer BFF 没有改动，根 `pom.xml` 分别复用阶段 03 和阶段 04 的模块；本目录只保留 Agent、Agent Eval 与 JDK 8 增量。

填写 `config/application.yml` 和 Ticket Agent Service 的下游地址后，在 IDEA 中运行 `KnowledgeServiceApplication`、`TicketAgentServiceApplication`、`CustomerBffApplication`。随后按第 26-34 篇通过 HTTP 创建任务、运行任务、确认高风险动作并读取最终回执。

JDK 8 边界使用独立的 `integrations/jdk8-client`，入口是 `TicketAgentClient`。它只依赖稳定 HTTP 契约，不依赖 Spring AI。
