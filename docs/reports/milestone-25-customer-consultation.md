# 里程碑 25：客户咨询与工单升级

状态：已完成本地接口与业务规则验证，仍需共享环境联调（`VERIFIED_LOCAL_CONTRACTS_SHARED_ENVIRONMENT_REQUIRED`）

## 已完成的能力

这一阶段已串起 Customer Web、客户 JWT 映射、带客户端认证的 RFC 8693 令牌交换、Knowledge HTTP/SSE 客户端、有界的短时会话、按回答尝试记录的反馈与重试、不可变工单交接快照，以及带委托身份和幂等校验的 Agent 任务接收。

## 已验证的行为

- Knowledge Service、Ticket Agent Service 与 Customer BFF 的本地测试覆盖会话、身份委托、下游协议、反馈、重试和工单升级。
- Customer Web 覆盖 SSE 分块解析、反馈、重试、转人工、类型检查和生产构建。
- OpenAPI、JSON Schema、正向与负向样例由项目契约测试统一校验。
- 敏感信息扫描覆盖已跟踪和未忽略文件，不把 API Key 写入仓库。

## 适用范围

本地测试和浏览器结果不能代表生产 IdP、网关 SSE、共享 Redis 或数据库会话存储、分布式限流、持久工单幂等、外部 pgvector 检索质量和端到端容量已经验收。这些结论需要在部署后的服务中，使用签名的短时令牌重新验证。
