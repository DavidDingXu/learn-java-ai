# 阶段 01：系统边界

本阶段只有三个真实 Spring Boot 进程，没有模型、数据库和教学包装层。

在 IDEA 中依次运行：

- `KnowledgeServiceApplication`，端口 `8081`
- `TicketAgentServiceApplication`，端口 `8082`
- `CustomerBffApplication`，端口 `8080`

三个 `/actuator/health` 都能独立返回健康状态。这里先确认部署和所有权边界，模型调用从阶段 02 才进入 Knowledge Service。

| 篇目 | 先看哪里 | 本篇只回答 |
|---|---|---|
| 01 | 三个 `Application` 和各自 health | 整套实战最终会长成哪些进程 |
| 02 | 三个模块的根包与 README | Knowledge、Agent、BFF 各自拥有什么 |
| 03 | 根 `pom.xml` 与三个模块 `pom.xml` | 为什么主线选 Spring AI 2.0.x，框架类型停在哪里 |
