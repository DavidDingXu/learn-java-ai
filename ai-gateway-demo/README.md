# ai-gateway-demo

`ai-gateway-demo` 演示 Java AI 项目的第一层工程边界：业务入口不直接调用模型 SDK，而是统一进入 `AiCallGateway`。

这个模块当前覆盖：

- `AiGatewayController`：只做 HTTP 协议适配。
- `AiCallGateway`：统一组装 Prompt、创建 trace、调用模型、处理超时、重试和降级。
- `AiGatewayAdvisorChain`：在模型调用前集中执行上下文增强、会话记忆等逻辑。
- `GatewayConversationMemoryAdvisor`：内存版会话记忆 Advisor，演示 Memory 不应该散落到 Controller。
- `ModelClient`：屏蔽具体模型 SDK。
- `ModelRouter`：按优先级选择主模型和备用模型。
- `AiCallLogEntry`：记录每次模型尝试的 model、attempt、status、latency、traceId 和 advisorEvents。
- `InMemoryAiCallLogRepository`：内存版调用日志仓储，用于示例和测试。

## 运行测试

在开源项目根目录执行：

```bash
mvn -pl ai-gateway-demo -am test
```

正常情况下会看到两个测试类都通过：

```text
Running com.xiaoding.javaai.gateway.AiCallGatewayTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
Running com.xiaoding.javaai.gateway.AiGatewayAdvisorChainTest
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

6 个测试分别验证：

- 正常模型调用会返回 `traceId`、`model`、`latencyMs`，并记录成功日志。
- 主模型第一次失败后会重试，第二次成功。
- 主模型连续失败后会切到备用模型，并记录每次失败和成功。
- 主模型超时后会记录失败原因，并切到备用模型。
- Advisor 会在模型调用前补充上下文，并把执行事件写入日志。
- 会话记忆 Advisor 只在模型网关边界补充最近会话，不让 Controller 直接拼历史消息。

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
mvn -pl ai-gateway-demo -am spring-boot:run
```

验证接口：

```bash
curl -X POST http://localhost:8081/api/ai/chat \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1001","message":"客户申请退款，但订单已经发货。"}'
```

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
