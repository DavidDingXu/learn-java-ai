# 架构边界复核

## Review Scope

本次复核只确认系统边界、所有权和集成方向是否足以支撑后续实现，不声称业务链路已经运行。

## Reviewed Files

- `pom.xml`
- `services/knowledge-service/pom.xml`
- `services/ticket-agent-service/pom.xml`
- `apps/customer-bff/pom.xml`
- `quality/eval-runner/pom.xml`
- `integrations/jdk8-client/pom.xml`
- `docs/architecture/system-context.md`
- `docs/architecture/customer-consultation-flow.md`
- `docs/architecture/service-ownership.md`
- `docs/adr/0002-build-boundaries.md`

## Accepted Boundaries

- 一条业务主线覆盖客户咨询、知识回答、工单升级、人工确认和老系统写入。
- Customer BFF 负责渠道和委托身份，不复制知识或工单领域。
- Knowledge Service 独占知识、检索、引用和模型回答职责。
- Ticket Agent Service 负责工单 AI 协同与受控动作，不拥有最终业务写权限。
- JDK8 CRM/工单系统继续作为业务状态和写操作的系统记录。
- Eval Runner 独立于服务实现，通过公开 HTTP API 执行评测。
- 服务间采用 HTTP/OpenAPI，不共享领域 JAR，也不跨服务访问数据库。

## Rejected Alternatives

- 拒绝把知识库和工单 Agent 拆成两个互不相干的演示项目：拆开后无法验证身份传递、失败升级和生产治理是否连贯。
- 拒绝让 Customer BFF 直接调用模型：这会把渠道层变成第二套知识服务。
- 拒绝建立共享领域模块：它会掩盖服务接口边界，并把内部模型耦合到 JDK8 客户端。
- 拒绝在没有消费方和交付语义前引入 Kafka、Outbox 或事件总线。
- 拒绝让 AI 服务直接写 CRM 数据库；高风险动作必须经过业务终端人工确认。

## Replacement Conditions

- 只有出现独立数据所有权、独立变化原因和独立发布需求时才新增服务。
- 只有出现削峰、重放、跨事务或异步扇出需求时才评估消息中间件。
- 只有稳定技术能力无法通过明确接口或局部重复解决时，才考虑共享库。

## Evidence Limit

这是代码结构和架构边界复核，不是运行实验。业务接口、委托身份、模型回答、工单升级和 JDK8 动作的实际行为，需要由对应的契约、集成或真实环境测试支持。
