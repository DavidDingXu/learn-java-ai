# 阶段 04 接口契约

- `openapi/customer-bff-v1.yaml`：回答、SSE、反馈、重试和工单接管。
- `openapi/agent-task-v1.yaml`：Ticket Agent 此时唯一实现的任务接收入口。
- `json-schema/agent-task-request-v1.schema.json` 与 fixtures：任务请求的正反例。

Knowledge Service 沿用阶段 03 的契约。Planner、运行、确认、审计和 Legacy Tool 从阶段 05 才出现，因此阶段 04 的 Agent Task 接口只包含 `POST /api/v1/agent/tasks`。
