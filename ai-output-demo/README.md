# ai-output-demo

这个模块演示 AI 结构化输出的 Java 后端边界。

主链路使用 Spring AI 原生能力：

- `PromptTemplate` 负责组装工单、制度和输出格式变量。
- `BeanOutputConverter<TicketAdviceResponse>` 负责生成 JSON Schema，并把模型响应转换成业务对象。
- `ChatClient.responseEntity(...)` 负责模型调用和结构化对象返回。
- `TicketAdviceResponse` 负责 Java 业务字段校验。

`AiJsonParser` 只保留为坏输出兜底入口，用来演示模型输出无法转成业务对象时，接口应该返回可识别的 400，而不是把 Spring 默认 500 JSON 暴露给页面。

## 核心类

```text
StructuredOutputController
TicketAdviceGenerationService
AiJsonParser
TicketAdviceResponse
RiskLevel
```

## 运行测试

在开源项目根目录执行：

```bash
mvn -pl ai-output-demo test
```

正常情况下会看到结构化输出相关测试通过，覆盖：

- Spring AI `BeanOutputConverter` 生成结构化输出格式约束。
- 生成入口返回 `TicketAdviceResponse` 业务对象。
- 合法工单建议 JSON 解析。
- 缺少风险等级。
- 缺少下一步动作。
- 中高风险建议缺少引用依据。
- 风险等级不是 Java 枚举值。
- 坏 JSON 拦截。
- 解析失败通过 HTTP 返回 400，而不是 500。

## 启动服务

默认不配置真实模型密钥时，生成入口会返回 deterministic sample，方便直接打开页面理解链路：

```bash
mvn -pl ai-output-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8082/
```

页面包含两个入口：

- 生成工单处理建议：调用 `/api/output/ticket-advice/generate`，展示 Spring AI 输出格式约束和业务对象。
- 坏输出兜底解析：调用 `/api/output/ticket-advice/parse`，验证非法 JSON 或非法业务字段会返回 400。

## 接入真实模型

配置 OpenAI-compatible 模型环境变量后重新启动：

```bash
export AI_API_KEY=your-api-key
export AI_BASE_URL=https://api.openai.com
export AI_CHAT_MODEL=gpt-4o-mini
mvn -pl ai-output-demo spring-boot:run
```

请求生成入口：

```bash
curl -X POST http://localhost:8082/api/output/ticket-advice/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "ticket": "客户申请退款，但订单已经发货。",
    "policy": "已发货订单需要先核对物流状态；高金额订单需要转人工复核。"
  }'
```

返回结构：

```json
{
  "mode": "model:gpt-4o-mini",
  "prompt": "...",
  "rawOutput": "...",
  "advice": {
    "summary": "...",
    "riskLevel": "MEDIUM",
    "nextActions": ["..."],
    "citations": ["..."]
  }
}
```

## 边界

- 不重新实现 Spring AI 的结构化输出能力。
- Java 代码补的是业务字段校验、错误 HTTP 契约、测试样本和后续 Trace / bad case / Eval 的接入位置。
- `AiCallGateway` 不参与这个主链路。结构化输出需要保留 Spring AI 的 `ChatClient` 和 `BeanOutputConverter` 能力，再在外层补业务校验、错误 HTTP 契约、trace 和 bad case 复盘。
