# Contracts

The public boundary is HTTP/OpenAPI first. Java service modules do not share a domain JAR with callers.

- `openapi/`: service-facing OpenAPI 3.1 documents.
- `json-schema/`: reusable business request schemas.
- `fixtures/`: positive and negative examples executed by Eval Runner.

Authentication and authorization facts come from verified tokens. Business request bodies never accept caller-supplied user, tenant, role or department values.

服务间 HTTP/OpenAPI 与 JSON Schema 的协议源文件放在这里。该目录不发布共享领域 Jar，服务也不能通过 Java 依赖共享领域对象。

第一版只采用 HTTP 合同，不预建消息协议、Kafka 或 Outbox。

当前公开合同覆盖 Customer BFF、Knowledge Service、Ticket Agent Service 与 JDK8 Legacy Tool。C 端请求只包含问题、会话标识、反馈和升级原因；租户、客户、角色与部门仍由令牌提供。
