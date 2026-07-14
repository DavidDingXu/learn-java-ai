# Customer Consultation Flow

## Purpose

这条流程把 C 端咨询、知识回答、工单升级和 JDK8 人工确认串成一个业务闭环。第一版选择同步 HTTP/OpenAPI，是因为每次调用都有明确发起方、结果接收方和超时边界；异步消息只在后续出现真实的削峰、重放或跨事务需求时引入。

## Target Sequence

```mermaid
sequenceDiagram
    actor Customer as 客户
    participant Web as Customer Web
    participant BFF as Customer BFF
    participant Knowledge as Knowledge Service
    participant Ticket as Ticket Agent Service
    participant CRM as JDK8 CRM / 工单系统

    Customer->>Web: 提交业务问题
    Web->>BFF: 客户令牌 + question
    BFF->>BFF: 校验渠道令牌并换取 delegated identity
    BFF->>Knowledge: POST /api/v1/knowledge/answers 或 /stream
    Knowledge->>Knowledge: 权限过滤、检索、模型回答、引用映射
    Knowledge-->>BFF: answer + citations + refusal + traceId
    alt 已解决
        BFF-->>Web: 流式或完整回答
    else 未解决或需要业务处理
        BFF->>Ticket: Idempotency-Key + 不可变升级快照
        Ticket-->>CRM: 返回建议动作和待确认任务
        CRM->>CRM: 客服人员核对权限、状态和参数
        CRM->>Ticket: 携带幂等键提交确认结果
        Ticket-->>CRM: 动作结果与审计编号
    end
```

当前已经实现 Customer BFF 的客户 JWT 边界、RFC 8693 Token Exchange、Knowledge HTTP/SSE 调用、短时会话、反馈、重试和幂等工单升级；Ticket Agent Service 已实现受限规划、Tool 目录、知识查询、风险分级、版本绑定确认、确认幂等、审计和 HTTP 下游适配器；独立 Java 8 客户端已能查询任务并提交确认。正式 Customer Web、生产持久化、真实 IdP、外部 JDK8 Tool 服务和完整端到端容量仍需公司环境验证。

## Contract Boundaries

1. Customer Web 只把客户令牌交给 BFF，不直接调用 Knowledge Service。
2. BFF 负责委托身份，不把 `tenantId`、`userId`、角色或部门塞进业务请求体。
3. Knowledge Service 依据签名令牌中的身份和文档权限生成回答；引用必须指向实际参与回答的上下文。
4. 拒答、用户主动要求人工处理或需要修改业务状态的问题可以进入工单流程；BFF 传递的是回答尝试快照，不自行判断业务权限。
5. Ticket Agent Service 生成的是受控建议和待确认任务，不拥有最终业务写权限。
6. JDK8 终端执行人工确认，并保持工单状态和业务写入的系统记录地位。

## Failure Branches

| Failure | Owner | Required behavior |
|---|---|---|
| 客户令牌无效 | Customer BFF | 在渠道边界拒绝，不调用下游 |
| 委托令牌无效或受众不匹配 | Knowledge / Ticket | 下游再次拒绝，不能信任 BFF 自报身份 |
| 模型未配置 | Knowledge Service | 返回明确的 `MODEL_NOT_CONFIGURED`，不能用空字符串冒充回答 |
| 缺少可引用依据 | Knowledge Service | 返回无法回答或升级信号，不能生成无来源结论 |
| 工单已关闭或状态冲突 | JDK8 CRM / Ticket | 拒绝动作并保留审计记录 |
| 重复确认请求 | Ticket / JDK8 client | 使用幂等键返回已有结果，不重复写业务状态 |
| 下游超时 | 调用方 | 按用例配置超时、重试和熔断；非幂等写操作不得自动盲重试 |

## Data Sharing Rule

服务协作以 HTTP/OpenAPI 合同为先，不共享领域 JAR，也不跨服务访问另一个服务的数据库。确需共享的只有版本化 OpenAPI、JSON Schema、错误码和测试夹具；这些合同不包含服务内部实体或持久化模型。
