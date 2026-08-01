# 服务职责与数据所有权

## 职责矩阵

| 组件 | 负责的能力 | 数据或状态 | 对外依赖 | 不应拥有 |
|---|---|---|---|---|
| Customer BFF | 客户渠道认证、delegated identity、协议聚合、短时会话 | 渠道会话和令牌交换上下文 | Knowledge/Ticket HTTP clients | 知识文档、工单状态、模型调用、业务写入 |
| Knowledge Service | 文档业务版本、检索版本、访问范围、引用和知识回答 | 知识元数据、原文、ACL、索引任务、分块与检索版本指针 | Model adapter、对象存储和向量库端口 | 工单工作流、CRM 写操作、客户渠道会话 |
| Ticket Agent Service | 工单 AI 协同、受控 Tool 编排、确认任务、幂等和审计 | Agent 任务、步骤、确认单、动作审计 | JDK8 Tool contract、Knowledge query contract | 知识源文件、客户令牌交换、CRM 数据库 |
| JDK8 CRM / Ticket System | 员工身份、最终工单状态、确认 UI、业务写入 | 现有 CRM/工单业务数据 | JDK8 HTTP/OpenAPI client | 模型编排、RAG、Agent runtime |
| Eval Runner | Golden Set、坏案例、接口结构校验、离线与在线评测报告 | 版本化数据集和报告 | 公开服务 API | 服务实现类、生产密钥、生产状态写入 |

## 依赖方向

```text
Customer Web -> Customer BFF -> Knowledge Service
                              -> Ticket Agent Service

JDK8 CRM -> JDK8 client -> Ticket Agent Service
Eval Runner -----------> public HTTP/OpenAPI contracts
Knowledge Service -----> business-specific model port -> Spring AI adapter
Ticket Agent Service --> ticket-planning port --------> Spring AI planner adapter
```

调用方向不等于数据所有权。BFF 可以聚合响应，但不能保存知识源或工单主状态；Ticket 可以请求知识上下文，但不能读取 Knowledge Service 数据库。

## 不可突破的规则

- 不共享领域 JAR。跨服务复用以 OpenAPI、JSON Schema、错误码、错误响应格式和测试夹具为限。
- 不跨服务读取或写入另一个服务的数据库，也不使用共享数据库作为集成方案。
- Controller 只处理协议、校验和身份上下文，不直接调用模型 SDK、向量库或其他服务数据库。
- Spring AI 和 Provider 类型只出现在 Knowledge Service、Ticket Agent Service 的基础设施层；应用层分别定义 `KnowledgeAnswerModel`、`TicketAgentPlanner` 等业务端口。
- 请求 DTO 不接受 `tenantId`、`userId`、角色或部门作为授权事实。
- 写操作必须经过权限、状态、参数、幂等和人工确认检查，并产生审计记录。
- Eval Runner 只通过公开 HTTP API 评测，不能成为生产调用链中的隐式依赖。

## 变更规则

只有组件拥有独立数据、独立变化原因和独立发布边界时，才拆成新服务。共享库只承载稳定的技术能力，不能持有领域所有权。引入消息中间件前，还要明确消费方、交付语义、重放策略和故障恢复责任。
