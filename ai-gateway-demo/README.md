# ai-gateway-demo

`ai-gateway-demo` 演示 Java AI 项目的第一层工程治理：Controller 不直接调用模型 SDK，普通 Chat 调用通过 `AiCallGateway` 承接路由、超时、重试、降级、日志和 trace。

这个模块的定位是“普通 Chat 调用治理样板”。它把 `traceId`、`model`、`attempt`、`latency`、`timeout`、`fallback` 和调用日志这些治理字段固定下来，方便后续专题复用同一套工程要求。

`AiCallGateway` 不接管 Spring AI 的高阶能力，也不是全项目的模型抽象层。结构化输出、Tool Calling、Advisor、Memory、Embedding、RAG、MCP 和模型响应元数据有自己的原生类型和生命周期，后续模块会保留 Spring AI 原生 API，再把同类治理项按场景接上。

这个模块当前覆盖：

- `AiGatewayController`：只做 HTTP 协议适配。
- `AiCallGateway`：统一组装 Prompt、创建 trace、调用模型、处理超时、重试和降级。
- `ModelClient`：屏蔽具体模型 SDK。
- `ModelRouter`：按优先级选择主模型和备用模型。
- `AiCallLogEntry`：记录每次模型尝试的 model、attempt、status、latency、traceId 和 errorMessage。
- `InMemoryAiCallLogRepository`：内存版调用日志仓储，用于示例和测试。

## 运行测试

在开源项目根目录执行：

```bash
mvn -pl ai-gateway-demo -am test
```

正常情况下会看到普通 Chat 治理和 HTTP 合同相关测试通过。

这些测试分别验证：

- 正常模型调用会返回 `traceId`、`model`、`latencyMs`，并记录成功日志。
- 主模型第一次失败后会重试，第二次成功。
- 主模型连续失败后会切到备用模型，并记录每次失败和成功。
- 主模型超时后会记录失败原因，并切到备用模型。
- HTTP 页面和接口只暴露普通 Chat 调用，后续结构化输出、Tool、Advisor、Memory 不复用这个文本入口。

## 启动接口

配置环境变量：

```bash
cp .env.example .env
set -a
source .env
set +a
```

启动服务：

```bash
mvn -pl ai-gateway-demo spring-boot:run
```

验证接口：

```bash
curl -X POST http://localhost:8081/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1001","message":"客户申请退款，但订单已经发货。"}'
```

也可以打开前端页面：

```text
http://localhost:8081/
```

页面会调用同一个普通 Chat 接口，并展示模型回复、Trace、模型名称和耗时。

返回结构：

```json
{
  "traceId": "...",
  "model": "gpt-4o-mini",
  "content": "...",
  "latencyMs": 1234
}
```

## 可以继续改的点

第一步可以把 `InMemoryAiCallLogRepository` 换成数据库表，字段先按 `AiCallLogEntry` 来设计。

第二步可以增加一个备用 `ModelClient`，覆盖 `priority()` 返回更大的值。主模型失败时，`AiCallGateway` 会继续尝试备用模型。

第三步可以把 `max-attempts` 调成 1 或 3，再观察 `AiCallGatewayTest` 里日志记录的变化。

第四步可以把 `call-timeout` 调小，例如 `30ms`，再看慢模型如何触发降级。真实项目里不要把超时时间写死在代码里，它应该是按场景配置出来的。

不要把这个模块继续扩成框架能力承载层。结构化输出看 `ai-output-demo`，Tool Calling 看 `ai-tool-demo`，Advisor / Memory 看 `ai-agent-demo` 里的 Spring AI 接入边界。后续复用的是治理字段和工程要求，不复用 `AiCallGateway.chat(...)` 这个普通文本接口。
