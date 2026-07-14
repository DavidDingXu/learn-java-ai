# Lesson 10 SSE Streaming Evidence

Status: VERIFIED_PROTOCOL_AND_SERVICE

- Implementation commit: `f5532160ae5018da237567e5855bd20bb7ce2123`
- Endpoint: `POST /api/v1/knowledge/answers/stream`
- OpenAPI: `contracts/openapi/knowledge-service-v1.yaml`

## Verified

- 业务事件稳定为 `metadata`、`delta`、`heartbeat`、`citation`、`completed` 和 `error`。
- 模型上游取消会随着客户端取消订阅传播。
- 心跳不改变业务状态；完成事件记录模型、usage、finish reason 和 TTFT。
- Controller 测试验证 `text/event-stream` 和事件名，应用测试验证事件顺序与取消。
- SSE 不应用自动重试，避免已经输出字节后产生重复内容。

## Evidence Boundary

本提交验证流式代码、HTTP 协议和取消语义，但没有把普通 `LIVE_MODEL` 报告冒充真实 SSE 报告。公司上线前仍需用目标 Provider、网关和前端执行断连、代理缓冲、空闲超时和慢客户端验收。
