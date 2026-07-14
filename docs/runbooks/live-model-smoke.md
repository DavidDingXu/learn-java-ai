# Live Model Smoke Runbook

## Purpose

该任务只验证一件事：Knowledge Service 能否通过当前 Spring AI 适配器调用一个真实的 OpenAI 兼容 Chat endpoint，并把回答、模型元数据、Token 用量和政策引用写入脱敏报告。它不验证 RAG、委托身份、工单、数据库或生产容量。

脚本直接调用应用端口，不经过 HTTP，因此不验证 JWT 或入站 Trace。HTTP Golden Set 和 Trace 证据由 `model-interaction-eval.md` 中的 live eval 提供。Knowledge Service 的公开接口默认仍受拒绝或 JWT 策略保护。

确定性模型协议合同进入默认测试，真实模型调用只在显式提供凭证时运行。前者证明请求与响应映射可回归，后者证明当前模型端点可调用；报告必须明确证据类型，不能互相替代。

## Required Environment

- `JAVA_AI_MAIN_JAVA_HOME`：完整 JDK21 或更新版本。
- `JAVA_AI_CHAT_API_KEY`：真实模型密钥。
- `JAVA_AI_CHAT_BASE_URL`：OpenAI 兼容 API 根地址，必须包含服务实际使用的 API 前缀，例如 `https://provider.example.com/v1`。网站首页返回 HTML 时不能作为该值。
- `JAVA_AI_CHAT_MODEL`：已在该账号和 endpoint 上开通的模型名。
- `JAVA_AI_LIVE_REPORT_PATH`：可选，默认覆盖 `docs/reports/lesson-04-live-model-smoke.md`。

密钥不能写入受版本控制的文件、报告或 Git。公司环境应通过密钥管理系统或受保护 CI Environment 注入；本地忽略的 `.env` 也应限制权限并定期轮换。

## macOS / Linux

```bash
export JAVA_AI_MAIN_JAVA_HOME=/path/to/jdk-21
export JAVA_AI_CHAT_API_KEY=***
export JAVA_AI_CHAT_BASE_URL=https://provider.example.com
export JAVA_AI_CHAT_MODEL=provider-model-name
scripts/run-live-model-smoke.sh
```

## Windows PowerShell

```powershell
$env:JAVA_AI_MAIN_JAVA_HOME = "C:\Java\jdk-21"
$env:JAVA_AI_CHAT_API_KEY = "***"
$env:JAVA_AI_CHAT_BASE_URL = "https://provider.example.com"
$env:JAVA_AI_CHAT_MODEL = "provider-model-name"
.\scripts\run-live-model-smoke.ps1
```

## Expected Result

脚本显式运行 `LiveModelSmokeIT`。成功条件包括：

- 返回非空回答。
- `executionMode` 等于 `LIVE_MODEL`。
- 至少有一条来自 `refund-policy/v1#arrival-time` 的引用。
- 模型名、finish reason 和 Token 用量能映射到项目响应。
- 模型输出通过 JSON Schema 转换、引用校验和业务动作校验。
- 报告不包含 API key 和 base URL。

缺少任一变量、JDK 不完整、Provider 鉴权失败、模型名错误、超时或响应映射失败时，脚本必须非零退出。

## Evidence Handling

报告写入 `docs/reports/lesson-04-live-model-smoke.md`，并绑定运行时的 Commit SHA。提交报告前需要复核回答中没有客户数据、内部 endpoint 或其他敏感信息。只有报告状态为 `LIVE_MODEL` 且对应提交已通过默认验证时，才允许创建 `milestone-04-real-model` 标签。
