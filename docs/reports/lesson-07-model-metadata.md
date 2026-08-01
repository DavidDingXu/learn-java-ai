# 模型响应元数据验证

Status: VERIFIED

- Application contract: `ModelUsage`, `ModelAnswerDraft`, `KnowledgeAnswer`
- Provider fixture: `ProviderProtocolFixtureTest`
- Live evidence: `lesson-04-live-model-smoke.md`

## 已验证

- Provider 模型名、Prompt Token、Completion Token、总 Token 和 finish reason 被映射为项目自有类型。
- 应用层和 Web 层不暴露 Spring AI 或 Provider SDK 类型。
- Provider 缺失 usage 时使用 0，不伪造成本；项目业务校验会拒绝完全缺失的 usage 对象。
- 模型端点报告记录模型、Token 和 finish reason。

## 替换边界

更换 Provider 或框架时只替换基础设施适配器，应用接口保持不变。不同 Provider 对缓存 Token、推理 Token 和 finish reason 的扩展字段需要单独建兼容测试，不能硬塞进通用字符串 Map。
