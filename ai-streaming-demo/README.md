# ai-streaming-demo

`ai-streaming-demo` 演示 Java AI 应用里的 SSE 流式输出边界。代码把模型回复抽象成可恢复、可观测的事件流，而不是只在 Controller 里返回一个 `Flux<String>`。

当前模块覆盖：

- `StreamingController`：把业务事件转换成 `ServerSentEvent`。
- `StreamSessionService`：生成 token / done 事件，支持 `Last-Event-ID` 断点恢复。
- `StreamEvent`：统一表达 SSE 的 `id`、`event`、`data`。
- `heartbeat(sessionId, sequence)`：生成心跳事件，避免把连接状态混进正文。
- `StreamMetrics`：记录请求开始时间、首 token 时间、完成时间、TTFT 和总耗时。

## 运行测试

在开源项目根目录执行：

```bash
mvn -pl ai-streaming-demo test
```

正常情况下会看到：

```text
Running com.xiaoding.javaai.streaming.StreamSessionServiceTest
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

4 个测试分别验证：

- 生成有序 token 事件和 done 事件。
- 使用 `Last-Event-ID` 从下一条事件恢复。
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

模拟断点恢复：

```bash
curl -N 'http://localhost:8083/api/stream/ticket-advice?sessionId=s1001' \
  -H 'Last-Event-ID: s1001-1'
```

第二个请求会从 `s1001-2` 开始返回。

## 可以继续改的点

第一步可以把事件 buffer 从内存 List 换成 Redis，并设置短 TTL。这样服务重启后仍然可以恢复最近一段流式输出。

第二步可以在 Controller 里混入定时 heartbeat，并让前端忽略 `heartbeat` 事件，只更新连接状态。

第三步可以把 `StreamMetrics` 写入 `ai-observability-demo` 的 trace 模型，让 TTFT、断连次数、恢复次数进入统一链路追踪。
