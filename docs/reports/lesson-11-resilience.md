# Lesson 11 Resilience Evidence

Status: VERIFIED_CONFIGURATION_WITH_MAPPING_BOUNDARY

- Implementation commit: `f5532160ae5018da237567e5855bd20bb7ce2123`
- Policy name: `knowledgeAnswer`
- Test: `ModelResilienceContractTest`

## Verified

- OpenAI 官方 Java SDK 的自动重试设为 0，Resilience4j 最多执行 2 次无业务副作用的模型调用尝试，避免重试倍增。
- 配置文件将超时设为 8 秒、并发上限设为 20，断路器使用计数窗口；当前契约测试只断言前两项与重试次数。
- `KnowledgeAnswerExceptionHandler` 实现了超时、断路器打开和并发满到 `MODEL_TIMEOUT`、`MODEL_CIRCUIT_OPEN`、`MODEL_BUSY` 的映射，但当前没有针对这三条映射的行为测试。
- 策略只包知识回答模型端口，不是跨 Chat、RAG、Tool 和 Agent 的全局网关。
- 本地模拟 Provider 验证了第一次返回 503 后由 Resilience4j 重试成功，以及慢响应触发 TimeLimiter 超时。

## Evidence Boundary

这些默认值用于示例工程，不是容量结论。当前证据没有覆盖超时后的连接释放、并发耗尽或断路器状态变化。公司项目需要根据 Provider SLO、线程模型、并发预算和上游超时补行为测试与压测；写操作 Tool 不得复用这里的模型生成重试策略。
