# API Contract

## Endpoint

两个主项目提供最小 REST 接口，方便本地运行和接口评审。

企业制度知识库：

```text
POST /api/enterprise-rag/documents
POST /api/enterprise-rag/answers
POST /api/enterprise-rag/eval
```

企业工单 AI 助手：

```text
GET  /api/helpdesk-agent/scenarios/refund
POST /api/helpdesk-agent/advice
POST /api/helpdesk-agent/tickets/close
```

## Request

接口请求必须带业务身份和租户上下文。真实生产项目中，这些字段来自登录态、网关或老系统传递的 `OperatorContext`。

关键字段：

- `tenantId`
- `operatorId`
- `departments`
- `ticketId`
- `question`
- `confirmationToken`

## Response

AI 接口响应必须返回业务结果和排障字段。

关键字段：

- `traceId`
- `status`
- `answer`
- `citations`
- `risk`
- `requiresHumanApproval`
- `toolCalls`
- `cost`
- `latencyMs`

错误响应要区分权限拒绝、无证据、证据冲突、模型失败和工具失败。
