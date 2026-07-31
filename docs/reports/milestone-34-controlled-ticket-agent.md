# Milestone 34 Controlled Ticket Agent

Status: VERIFIED_LOCAL_CONTROLLED_AGENT_LIVE_MODEL_SHARED_ENVIRONMENT_REQUIRED

## 已完成的能力

The milestone contains a bounded Agent task state machine, Spring AI 2.0 structured planning, server-owned Tool Catalog, delegated Knowledge read tool, risk-classified write tools, version-bound human confirmation, remote result classification, Java 8 task client, auditable OpenAPI and independent Agent evaluation.

## 已验证的行为

- Real model planner smoke passed with a structured `USE_TOOL / QUERY_KNOWLEDGE` decision and model usage metadata.
- Java 主工程、隔离框架 labs 和独立 Java 8 客户端都有可重复执行的验证入口。
- Agent 路径评测检查工具、参数、风险、角色和确认前禁止事件。
- 确定性业务测试覆盖未知工具、非法参数、拒绝、过期与重复确认，以及幂等冲突。
- 敏感信息扫描不允许 API Key 和服务令牌进入仓库。

## External Boundary

Local verification does not prove production IdP, persistent Agent task/confirmation/audit storage, prepared external Knowledge index, durable Legacy Tool idempotency, uncertain-result reconciliation or end-to-end capacity. These must be verified in the company shared environment before enabling write tools.
