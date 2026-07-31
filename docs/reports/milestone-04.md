# 里程碑 04：第一条真实模型调用

状态：已完成真实模型验证（`VERIFIED_LIVE_MODEL`）

## 已验证的行为

- 应用服务、HTTP 校验和默认禁用模型分支已通过测试。
- OpenAI 兼容协议回归测试已验证请求映射、回答、Token 用量、结束原因和引用。
- 默认验证不需要模型密钥、数据库或 Docker。
- `docs/reports/lesson-04-live-model-smoke.md` 记录显式 OpenAI 兼容端点调用，状态为 `LIVE_MODEL`。
- 报告包含模型名、Token、结束原因和引用，且不包含密钥或服务地址。

## 适用范围

- 该报告验证固定政策上下文、结构化回答和 Spring AI 适配器，不代表向量 RAG、生产身份、容量或流式链路已经验收。
- 确定性模型协议回归保留在默认测试中，用于无密钥检查请求与响应映射，不能替代 Provider API Smoke 和评测。
