# 接口契约

服务间以 HTTP/OpenAPI 作为公开边界，不通过共享领域 JAR 传递 Java 对象。

- `openapi/`：Customer BFF、Knowledge Service、Ticket Agent Service 与 JDK8 Legacy Tool 的 OpenAPI 3.1 文档。
- `json-schema/`：可独立校验的业务请求和模型输出结构。
- `fixtures/`：由 Eval Runner 和仓库契约测试执行的正反例。

身份与授权信息只来自已验证的 Token。业务请求体不接收调用方传入的用户、租户、角色或部门字段。C 端契约只表达问题、会话、反馈和升级原因；信任身份由 BFF 委托。

当前交互采用版本化 HTTP/OpenAPI。项目没有为未实现的消费方预建 Kafka、Outbox 或共享事件模型。

仓库的作者侧验收会检查身份字段、幂等键、错误响应和 Java 8 兼容性。读者沿文章操作时不需要先运行这些测试；启动对应应用后，直接通过文章给出的 HTTP 请求观察契约是否生效。
