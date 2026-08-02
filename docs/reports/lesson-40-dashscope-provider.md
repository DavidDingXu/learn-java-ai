# DashScope Provider 替换验证

Status: VERIFIED_ISOLATED_LAB


## 已验证

- `DashScopeProviderAdapter` maps the stable business request to real Spring AI Alibaba `DashScopeChatOptions`.
- 适配器返回项目自有的 `ProviderAnswer`，不对外暴露 Spring AI 响应类型。
- 测试覆盖 Prompt/Options 映射、配置模型的缺省处理，并确认业务响应不包含 API Key 字段。

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## 外部验证边界

该实验尚未归一化 usage 与 finish reason，也没有把 Provider 异常映射为稳定应用错误，更不代表已完成真实 DashScope 调用。投入生产前还要补充适配器测试，并使用真实凭证验证模型回归、限流、内容审核和 usage。
