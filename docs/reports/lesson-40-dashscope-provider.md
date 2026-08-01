# DashScope Provider 替换验证

Status: VERIFIED_ISOLATED_LAB


## 已验证

- `DashScopeProviderAdapter` maps the stable business request to real Spring AI Alibaba `DashScopeChatOptions`.
- The adapter returns a project-owned `ProviderAnswer` instead of exposing Spring AI response types.
- Tests cover prompt/option mapping, configured-model fallback and the absence of an API-key field in the business response.

## 验证命令

```bash
./mvnw -f labs/pom.xml -pl spring-ai-alibaba-lab test
```

## 外部验证边界

The lab does not normalize usage or finish reasons and does not map provider exceptions into stable application errors. It also does not claim a live DashScope call. Production adoption still requires those adapter tests plus real credentials, model regression, rate-limit, content-filter and usage verification.
