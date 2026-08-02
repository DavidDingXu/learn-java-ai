# 超时、限流与降级验证

Status: VERIFIED_OPERATION_SPECIFIC_RESILIENCE


## 已验证

- Knowledge 同步模型调用的韧性策略只作用于业务模型端口，不套用写操作重试语义。
- Knowledge 保留一层显式 Resilience4j 重试；Knowledge 与 Ticket 都关闭 OpenAI SDK 重试，避免重复承担重试责任。
- 真实 Spring 上下文与本地 Provider 协议服务验证了 Knowledge 适配器代理、单次 503 重试和慢响应超时。
- 客户咨询链路保留渠道级固定窗口限流。
- BFF 流式客户端设置空闲超时并保留取消信号；Knowledge Service 会把订阅取消继续传递给模型流。
- Ticket Agent Run 使用公平信号量控制准入，并允许配置并发上限。
- 容量耗尽时返回 HTTP 429 和稳定错误码 `AGENT_RUN_CAPACITY_EXCEEDED`。
- 成功和异常都会释放许可；Tool 写入仍分别处理明确拒绝和远程结果不确定。

## 验证命令

```bash
./mvnw -pl services/knowledge-service,services/ticket-agent-service,apps/customer-bff \
  -Dtest=ModelResilienceContractTest,ProviderProtocolFixtureTest,TicketAgentModelConfigurationTest,SemaphoreAgentRunAdmissionTest,AgentTaskExceptionHandlerTest,InMemoryFixedWindowConsultationRateLimiterTest,WebClientKnowledgeAnswerStreamClientTest,StreamingKnowledgeAnswerServiceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 生产接入边界

信号量按实例生效，BFF 限流器使用进程内存。当前流式链路没有端到端总时限，流式模型适配器也不使用同步模型的 Resilience4j 注解。接口样例未覆盖断路器从关闭、打开到半开的完整恢复过程。公司上线时应根据压测确定参数，只在必要时增加共享租户或全局配额；写 Tool 的重试仍必须绑定业务幂等和结果查询。
