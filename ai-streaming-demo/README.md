# ai-streaming-demo

`ai-streaming-demo` 演示 Java AI 应用里的 SSE 流式输出边界。代码把模型回复抽象成可恢复、可观测的事件流，而不是只在 Controller 里返回一个 `Flux<String>`。

`/api/stream/ticket-advice/live` 会通过 Spring AI `ChatClient.stream()` 调真实模型并输出 SSE `model-token` 事件。未配置有效 `AI_API_KEY` 时，live 入口返回 `AI_CONFIGURATION_REQUIRED`。

当前模块覆盖：

- `StreamingController`：把业务事件转换成 `ServerSentEvent`。
- `StreamSessionService`：生成 token / done 事件，支持 `Last-Event-ID` 和 `lastEventId` 断点恢复。
- `StreamEvent`：统一表达 SSE 的 `id`、`event`、`data`。
- `heartbeat(sessionId, sequence)`：生成心跳事件，避免把连接状态混进正文。
- `StreamMetrics`：记录请求开始时间、首 token 时间、完成时间、TTFT 和总耗时。

## 运行测试

在开源项目根目录执行：

```bash
mvn -pl ai-streaming-demo test
```

正常情况下会看到 `StreamSessionServiceTest`、`StreamingControllerTest` 和应用上下文测试通过，覆盖：

- 生成有序 token 事件和 done 事件。
- 使用 `Last-Event-ID` 或 `lastEventId` 从下一条事件恢复。
- 心跳事件使用独立的 `heartbeat` 类型。
- TTFT 和总耗时按时间戳计算。

## 启动接口

```bash
mvn -pl ai-streaming-demo spring-boot:run
```

验证流式输出：

```bash
curl -N 'http://localhost:8083/api/stream/ticket-advice?sessionId=s1001'
```

也可以打开前端页面：

```text
http://localhost:8083/
```

页面会用 SSE 逐字显示 token，并展示事件 ID、done 事件和连接状态。

模拟断点恢复：

```bash
curl -N 'http://localhost:8083/api/stream/ticket-advice?sessionId=s1001' \
  -H 'Last-Event-ID: s1001-1'
```

第二个请求会从 `s1001-2` 开始返回。

配置真实模型后，可以验证真实模型流：

```bash
curl -N 'http://localhost:8083/api/stream/ticket-advice/live?question=客户申请退款但订单已经发货，应该怎么处理？'
```

重点看 SSE 事件里的 `event:model-token` 和最终 `event:done`。

如果在页面里手动演示恢复，可以把 `sessionId` 保持为 `s1001`，把 `lastEventId` 填成 `s1001-1` 或 `s1001-2`。接口也兼容 `sessionId=s1001-2` 这种输入，会把它解释成从 `s1001` 会话的第二个事件之后恢复。

## 可以继续改的点

第一步可以把事件 buffer 从内存 List 换成 Redis，并设置短 TTL。这样服务重启后仍然可以恢复最近一段流式输出。

第二步可以在 Controller 里混入定时 heartbeat，并让前端忽略 `heartbeat` 事件，只更新连接状态。

第三步可以把 `StreamMetrics` 写入 `ai-observability-demo` 的 trace 模型，让 TTFT、断连次数、恢复次数进入统一链路追踪。
