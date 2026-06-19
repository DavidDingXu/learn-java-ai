# ai-common

共享基础模型模块。

这个模块不单独启动，也不直接调用模型。它只放跨 demo 复用的轻量对象，避免每个模块都重复定义最基础的请求、响应和 trace 数据结构。

## 当前包含什么

```text
AiChatRequest
AiChatResponse
AiTrace
AiTraceEvent
```

`AiChatRequest` 会校验 `userId` 和 `message` 不能为空。这个校验很小，但它表达了一个基本边界：进入 AI 链路前，请求对象不能是空的。

`AiTrace` 和 `AiTraceEvent` 是早期共享 trace 对象。更完整的全链路追踪实现放在 `ai-observability-demo`，那里会继续扩展 span、event、model usage、quota 和 feedback。

## 测试

```bash
mvn -pl ai-common test
```

正常情况下会看到基础模型测试通过，覆盖空消息拒绝。

## 边界

- 本模块不是模型网关，模型调用在 `ai-gateway-demo`。
- 本模块不是完整 trace 系统，生产治理示例在 `ai-observability-demo`。
- 本模块不依赖 Spring Boot，便于被其他 demo 直接引用。
