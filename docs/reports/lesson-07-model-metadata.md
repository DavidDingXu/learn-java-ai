# Lesson 07 Model Metadata Evidence

Status: VERIFIED

- Implementation commit: `f5532160ae5018da237567e5855bd20bb7ce2123`
- Application contract: `ModelUsage`, `ModelAnswerDraft`, `KnowledgeAnswer`
- Provider fixture: `ProviderProtocolFixtureTest`
- Live evidence: `lesson-04-live-model-smoke.md`

## Verified

- Provider 模型名、Prompt Token、Completion Token、总 Token 和 finish reason 被映射为项目自有类型。
- 应用层和 Web 层不暴露 Spring AI 或 Provider SDK 类型。
- Provider 缺失 usage 时使用 0，不伪造成本；项目业务校验会拒绝完全缺失的 usage 对象。
- 真实模型报告已记录模型、Token 和 finish reason。

## Replacement Boundary

更换 Provider 或框架时只替换基础设施适配器，应用合同保持不变。不同 Provider 对缓存 Token、推理 Token 和 finish reason 的扩展字段需要单独建兼容测试，不能硬塞进通用字符串 Map。
