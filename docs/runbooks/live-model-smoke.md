# 真实模型冒烟验证

## 验证目标

该任务只检查一件事：Knowledge Service 能否通过当前 Spring AI 适配器调用配置的 OpenAI 兼容 Chat endpoint，并把回答、模型元数据、Token 用量和政策引用写入脱敏报告。它不检查 RAG、委托身份、工单、数据库或生产容量。

冒烟请求经过 Knowledge Service 的公开 HTTP 接口，因此能同时观察固定本地身份、Trace、模型元数据和响应校验。需要访问完整 RAG 时，再将 `java-ai.knowledge.mode` 改为 `postgres-rag`，并准备 PostgreSQL、Embedding 和检索数据；本地不需要 JWT。

## 本地演示配置

项目根目录的 `config/application.yml` 已给出 OpenAI API 地址、演示模型和不可用的占位 Key。使用 OpenAI 时只需替换这一项：

```yaml
spring:
  ai:
    openai:
      api-key: replace-with-your-api-key
```

如果使用其他 OpenAI 兼容服务，再在同一文件中修改 `base-url`、`chat.model` 和 `embedding.model`。只要当前终端使用完整 JDK 21 或更新版本，就不需要配置 Java 环境变量。

本地文件使用明文是为了降低演示门槛。填入真实 Key 后不能提交，运行结束后应恢复占位值。生产环境必须通过 Secret Manager、Vault 或部署平台 Secret 覆盖 `spring.ai.openai.api-key`，不能把真实密钥保存在仓库或镜像中。

## 直接运行两个 main 方法

在 IDE 中把 Working directory 设为项目根目录，先运行 `KnowledgeServiceApplication`。服务健康后，运行 `EvalRunner.main()`，Program arguments 填写：

```text
model-eval --dataset datasets/model-interaction/golden-set-v2.jsonl --base-url http://localhost:8081 --mode LIVE_MODEL --prompt-version knowledge-answer-v1 --environment-id local-live-model --report var/learning-stage-reports/live-model-smoke
```

macOS 和 Windows 使用相同的 Java 入口与参数。Runner 会调用已经启动的真实服务，并在 `var/learning-stage-reports` 下生成 JSON 与 Markdown 报告。

## 成功条件

运行成功应同时满足：

- 返回非空回答。
- 响应包含非空模型名、Token 用量、结束原因、Trace 和有效引用。
- 至少有一条来自 `refund-policy/v1#arrival-time` 的引用。
- 模型名、finish reason 和 Token 用量能映射到项目响应。
- 模型输出通过 JSON Schema 转换、引用校验和业务动作校验。
- 报告不包含 API key 和 base URL。

API Key 仍是占位值、Provider 鉴权失败、模型名错误、超时或响应映射失败时，Runner 会明确失败。

## 报告边界

报告写入 `var/learning-stage-reports/live-model-smoke.md`，不会保存 API Key 和 Provider 地址；对外分享前仍需检查模型回答中是否包含客户数据或内部信息。`docs/reports` 中的发布证据由维护者流程生成，不是读者首次启动的前置条件。
