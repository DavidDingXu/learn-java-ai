# Lesson 22 Thin Customer BFF Evidence

Status: VERIFIED_LOCAL_CHANNEL_BOUNDARY

Implementation commit: `2cbe5398e6cfec9090bed091947f6b0d261077ee`

## Verified

- Customer BFF 只接收 `conversationId` 和 `question`，客户、租户、角色与部门只从已验证 JWT 映射。
- BFF 使用 RFC 8693 Token Exchange 为 Knowledge Service 与 Ticket Agent Service 分别申请 audience 和 scope 不同的短期令牌。
- Token Exchange 支持客户端 Basic 认证、显式超时和表单协议；下游仍独立校验签名、issuer、audience、actor、tenant 与 scope。
- 完整回答和 SSE 都通过 WebClient 调用 Knowledge Service；BFF 不引入 Spring AI，也不复制检索、Prompt 或回答校验规则。
- SSE 公开稳定的 `session`、`metadata`、`delta`、`heartbeat`、`citation`、`completed` 和 `error` 事件。
- 流式尝试只有收到 `completed` 后才进入完成状态；断流、错误或取消都会把本次尝试标记为失败。
- 单机限流键由 JWT 中的租户与客户主体组成，请求体无法伪造限流身份。

## Local Verification

```bash
./mvnw -pl apps/customer-bff \
  -Dtest=CustomerBffApplicationTest,CustomerJwtIdentityFactoryTest,OAuth2TokenExchangeDelegatedTokenClientTest,WebClientKnowledgeAnswerClientTest,WebClientKnowledgeAnswerStreamClientTest,CustomerConsultationControllerTest,InMemoryFixedWindowConsultationRateLimiterTest \
  test
```

## Production Boundary

进程内限流只适合单实例学习和规则验证，多实例环境必须替换为共享限流实现。公司 IdP、网关缓冲、浏览器断连、跨服务超时预算和最大并发仍需在测试环境完成联调与压测。
