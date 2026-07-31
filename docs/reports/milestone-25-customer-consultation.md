# Milestone 25 Customer Consultation

Status: VERIFIED_LOCAL_CONTRACTS_SHARED_ENVIRONMENT_REQUIRED

## 已完成的能力

The milestone contains Customer Web, customer JWT mapping, authenticated RFC 8693 Token Exchange, Knowledge HTTP and SSE clients, a bounded short-lived conversation aggregate, attempt-scoped feedback and retry, immutable ticket handoff snapshots, delegated ticket identity and idempotent Ticket Agent task intake.

## 已验证的行为

- Knowledge Service、Ticket Agent Service 与 Customer BFF 的本地测试覆盖会话、身份委托、下游协议、反馈、重试和工单升级。
- Customer Web 覆盖 SSE 分块解析、反馈、重试、转人工、类型检查和生产构建。
- OpenAPI、JSON Schema、正向与负向样例由项目契约测试统一校验。
- 敏感信息扫描覆盖已跟踪和未忽略文件，不把 API Key 写入仓库。

## External Boundary

The local test and browser results do not prove a production IdP, gateway SSE behavior, shared Redis or database session storage, distributed rate limiting, durable ticket idempotency, external pgvector retrieval quality or end-to-end capacity. The runtime configuration provides the real integration boundary; those conclusions require deployed services and signed short-lived tokens.
