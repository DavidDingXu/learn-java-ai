# Lesson 11 Resilience Evidence

Status: VERIFIED_CONFIGURATION_AND_MAPPING

- Implementation commit: `f5532160ae5018da237567e5855bd20bb7ce2123`
- Policy name: `knowledgeAnswer`
- Test: `ModelResilienceContractTest`

## Verified

- Spring AI 内建重试限制为 1 次，业务适配器最多执行 2 次安全读重试，避免重试倍增。
- 超时为 8 秒，并发上限为 20，断路器使用计数窗口。
- 超时、断路器打开和并发满分别映射为稳定错误码 `MODEL_TIMEOUT`、`MODEL_CIRCUIT_OPEN`、`MODEL_BUSY`。
- 策略只包知识回答模型端口，不是跨 Chat、RAG、Tool 和 Agent 的全局网关。

## Evidence Boundary

这些默认值用于示例工程，不是容量结论。公司项目必须根据 Provider SLO、线程模型、并发预算和上游超时重新压测。写操作 Tool 不得复用这里的读重试策略。
