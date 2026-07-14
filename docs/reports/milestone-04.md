# Milestone 04

Status: VERIFIED_LIVE_MODEL

## Verified

- 应用服务、HTTP 校验和默认禁用模型分支已通过测试。
- OpenAI 兼容 provider protocol fixture 已验证请求映射、回答、usage、finish reason 和引用。
- 默认验证不需要模型密钥、数据库或 Docker。
- `docs/reports/lesson-04-live-model-smoke.md` 已使用真实 OpenAI 兼容服务完成调用，状态为 `LIVE_MODEL`。
- 报告绑定实现提交 `f5532160ae5018da237567e5855bd20bb7ce2123`，包含模型名、Token、finish reason 和引用，且不包含密钥或服务地址。

## Evidence Boundary

- 该报告验证固定政策上下文、结构化回答和 Spring AI 适配器，不代表向量 RAG、生产身份、容量或流式链路已经验收。
- 确定性模型协议合同保留在默认测试中，用于无密钥回归；它只证明请求与响应映射，不能替代真实模型验证。

## Tag Rule

`milestone-04-real-model` 必须指向实现提交 `f5532160ae5018da237567e5855bd20bb7ce2123`，而不是后续文章或报告提交。
