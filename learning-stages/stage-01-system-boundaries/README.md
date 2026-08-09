# 阶段 01：系统边界

本阶段只有三个真实 Spring Boot 进程，没有模型、数据库和教学包装层。

在 IDEA 中依次运行：

- `KnowledgeServiceApplication`，端口 `8081`
- `TicketAgentServiceApplication`，端口 `8082`
- `CustomerBffApplication`，端口 `8080`

三个 `/actuator/health` 都能独立返回健康状态。这里先确认部署和所有权边界，模型调用从阶段 02 才进入 Knowledge Service。

