# Live Model Smoke Runbook

## Purpose

该任务只检查一件事：Knowledge Service 能否通过当前 Spring AI 适配器调用配置的 OpenAI 兼容 Chat endpoint，并把回答、模型元数据、Token 用量和政策引用写入脱敏报告。它不检查 RAG、委托身份、工单、数据库或生产容量。

脚本直接调用应用用例，不经过 HTTP，因此不验证 JWT 或入站 Trace。HTTP Golden Set 和 Trace 证据由 `model-interaction-eval.md` 中的 live eval 提供。Knowledge Service 的公开接口默认仍受拒绝或 JWT 策略保护。

确定性协议回归进入默认测试，模型端点调用只由下面的独立脚本触发。前者检查请求与响应映射，后者检查当前端点能否调用；两项结果不能互相替代。

该脚本不是应用启动入口。它自动选择 JDK、加载本地模型配置、运行指定 Java 集成测试并生成报告。需要手工访问 HTTP 接口时，应启动 Knowledge Service；那条路径还需要数据库、JWT 和检索等完整系统配置。

## 本地演示配置

项目根目录的 `config/application.yml` 已给出 OpenAI API 地址和演示模型。使用 OpenAI 时，只需把下面的占位值换成自己的 API Key：

```yaml
spring:
  ai:
    openai:
      api-key: replace-with-your-api-key
```

如果使用其他 OpenAI 兼容服务，再在同一文件中修改 `base-url`、`chat.model` 和 `embedding.model`。脚本会自动寻找本机已安装的完整 JDK 21 或更新版本，不需要配置 Java 环境变量。

这份 YAML 采用明文占位值是为了降低本地演示门槛。不要提交填写了真实密钥的文件。生产环境必须通过 Secret Manager、Vault 或部署平台 Secret 覆盖 `spring.ai.openai.api-key`，不能把真实密钥保存在仓库或镜像中。

## macOS / Linux

```bash
scripts/run-live-model-smoke.sh
```

## Windows PowerShell

```powershell
.\scripts\run-live-model-smoke.ps1
```

## Expected Result

脚本显式运行 `LiveModelSmokeIT`。成功条件包括：

- 返回非空回答。
- 响应包含非空模型名、Token 用量、结束原因、Trace 和有效引用。
- 至少有一条来自 `refund-policy/v1#arrival-time` 的引用。
- 模型名、finish reason 和 Token 用量能映射到项目响应。
- 模型输出通过 JSON Schema 转换、引用校验和业务动作校验。
- 报告不包含 API key 和 base URL。

API Key 仍是占位值、未安装完整 JDK、Provider 鉴权失败、模型名错误、超时或响应映射失败时，脚本都会非零退出。

## Evidence Handling

报告写入 `docs/reports/lesson-04-live-model-smoke.md`，并记录运行时的 Commit SHA。报告不会写入 API Key 和 Provider 地址；对外分享前仍需检查模型回答中是否包含客户数据或内部信息。
