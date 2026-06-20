# project-helpdesk-agent

这是企业工单 AI 助手主项目。

它围绕企业工单处理展开，把 Agent 参与业务执行时必须处理的生产边界放进代码：

- 工单查询
- 订单查询
- 制度检索
- 处理建议生成
- 高风险动作人工确认
- Tool 调用审计
- Agent Trace
- Agent Eval

当前第一版使用内存仓储，方便读者直接运行测试。它已经把工单上下文、Tool API、人工确认、确认 token、幂等、状态检查、Tool 审计、Agent Trace 和 Agent Eval 这些边界落到代码里。MySQL、Redis、真实制度 RAG、真实模型建议和结构化输出还没有接入，后续应该通过仓储、API client 和模型网关替换，不改 Agent 的受控执行边界。

## 核心类

```text
HelpdeskAgentApplicationService // 工单 Agent 应用编排入口
AgentAdviceRequest              // 处理建议请求
AgentAdviceResult               // 建议、风险、引用、trace、tool records
HelpdeskAgentScenarioReport     // 端到端场景报告，方便测试和接口演示完整结果
TicketToolFacade                // Agent 可调用的受控 Tool 门面
ToolExecutionLedger             // Tool 调用审计账本
PolicyKnowledgeBase             // 制度检索边界
AiTrace / TraceStep             // Agent 执行链路追踪
AgentEvalCase / AgentEvalReport // Agent 评测
```

## 运行测试

```bash
mvn -pl project-helpdesk-agent test
```

正常情况下会看到 10 个测试通过，覆盖领域编排、Tool 审计、写操作确认与幂等、Agent Eval 和 REST 接口。

## 启动服务

```bash
mvn -pl project-helpdesk-agent spring-boot:run
```

默认端口是 `8091`。

打开前端页面：

```text
http://localhost:8091/
```

页面会跑退款工单场景，展示建议生成、引用、Tool 审计、Trace 和人工确认关闭。

## 跑通退款工单场景

```bash
curl http://localhost:8091/api/helpdesk-agent/scenarios/refund
```

重点看这些字段：

```text
ticketId = T-1001
riskLevel = HIGH
requiredAction = MANUAL_REVIEW
citationDocumentIds = refund-policy-2026
toolNames = ticket.lookup, order.lookup, policy.search
traceStepNames = ticket.lookup, order.lookup, policy.search, advice.compose, approval.plan
```

也可以直接提交一个建议请求：

```bash
curl -X POST http://localhost:8091/api/helpdesk-agent/advice \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "question": "客户申请退款，但订单已经发货，应该怎么处理？",
    "userId": "u1001",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

关闭工单接口会要求人工确认：

```bash
curl -X POST http://localhost:8091/api/helpdesk-agent/tickets/close \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "humanApproved": false,
    "userId": "u1001",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

返回里的 `success` 应该是 `false`，`message` 会说明需要人工确认。

人工确认后还要传入确认 token，用它做幂等控制：

```bash
curl -X POST http://localhost:8091/api/helpdesk-agent/tickets/close \
  -H 'Content-Type: application/json' \
  -d '{
    "ticketId": "T-1001",
    "humanApproved": true,
    "confirmationToken": "confirm-close-1001",
    "userId": "lead-1",
    "tenantId": "tenant-a",
    "department": "support"
  }'
```

相同 `confirmationToken` 重复提交不会重复关闭工单；如果工单已经关闭，再换一个 token 提交，会返回已关闭，避免绕过状态检查。

当前最适合先看的测试是：

```text
HelpdeskAgentProjectTest#generatesReadableScenarioReportForEndToEndHelpdeskAgent
```

它会跑通退款工单场景，并检查：

```text
ticketId = T-1001
riskLevel = HIGH
requiredAction = MANUAL_REVIEW
citationDocumentIds = refund-policy-2026
toolNames = ticket.lookup, order.lookup, policy.search
traceStepNames = ticket.lookup, order.lookup, policy.search, advice.compose, approval.plan
```

## 已覆盖的生产边界

- Agent 先查工单，再查订单，再检索制度，最后生成建议。
- 制度检索按租户和部门过滤，客服不能读取 HR 制度。
- 关闭工单属于写操作，必须人工确认、确认 token、幂等和状态检查。
- Tool 调用会进入 `ToolExecutionLedger`。
- Agent 每个关键步骤都会进入 `AiTrace`。
- Eval 同时评估引用命中和动作判断。

## 分阶段演进

这个项目的重点是让 Agent 在业务系统里受控执行。

### 阶段 1：内存版边界

当前已经完成：

- Agent 先查工单，再查订单，再检索制度，最后生成建议。
- 制度检索按租户和部门过滤。
- 关闭工单属于写操作，必须人工确认。
- 人工确认必须带 `confirmationToken`。
- 相同 token 重复提交不会重复执行。
- 工单已关闭后不能换 token 再次关闭。
- Tool 调用进入 `ToolExecutionLedger`。
- Agent 关键步骤进入 `AiTrace`。
- Eval 同时评估引用命中和动作判断。

### 阶段 2：基础设施替换

推荐顺序：

1. 工单、订单、审计记录进入 MySQL。
2. 会话上下文、确认 token、短期缓存进入 Redis。
3. 人工确认记录独立成审批表。
4. Tool 调用记录按 `traceId` 查询，而不是从内存账本取 offset。

替换时继续保留 Tool 边界。Agent 通过 `TicketToolFacade` 这类受控门面使用业务能力，不直接操作业务 Service。

### 阶段 3：真实 AI 能力

推荐顺序：

1. 制度检索接入 `project-enterprise-rag` 的 REST API。
2. 建议生成接入 `ai-gateway-demo` 的模型网关。
3. 处理建议输出接入 `ai-output-demo` 的结构化解析和修复。
4. 需要流式体验时，再接入 WebFlux / SSE。

关闭工单、退款、转派这类写操作不由模型直接执行。模型可以生成建议，写操作必须经过权限、状态、幂等和人工确认。

### 阶段 4：治理和上线

后续要接入：

- `ai-eval-demo` 的 Agent golden set 和工具路径评测。
- `ai-observability-demo` 的 trace、成本、限流和坏 case。
- 多租户配额。
- 高风险动作抽检。
- 发布验收和回滚计划。
