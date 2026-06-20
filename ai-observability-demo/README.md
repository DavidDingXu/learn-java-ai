# ai-observability-demo

AI 全链路追踪 demo。

本模块演示一次 AI 请求如何记录 Trace、Prompt/RAG/Model/Tool/Agent Span、模型 token 成本、用户/租户配额、坏 case 反馈，以及上线前的发布验收检查。

## 启动

```bash
mvn -pl ai-observability-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8088/
```

页面会生成 Trace 时间线，记录 Prompt、RAG、Tool、Agent Step、成本、限流和坏 case 反馈。

## 创建 Trace

```bash
curl -X POST http://localhost:8088/api/traces \
  -H 'Content-Type: application/json' \
  -d '{"userId":"u1001","scenario":"ticket-advice"}'
```

## 记录 Span

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/spans \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "rag.retrieve",
    "type": "RAG",
    "attributes": {
      "chunks": 2,
      "documentIds": ["refund-policy-001"]
    }
  }'
```

## 记录结构化 AI 步骤

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/prompt \
  -H 'Content-Type: application/json' \
  -d '{"templateVersion":"ticket-advice-v3","variables":["ticketId","operatorRole"]}'
```

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/rag \
  -H 'Content-Type: application/json' \
  -d '{
    "query": "发货后退款",
    "chunkIds": ["refund-policy-001-c1"],
    "scores": ["0.92"]
  }'
```

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/tool \
  -H 'Content-Type: application/json' \
  -d '{"toolName":"order.lookup","argsDigest":"orderId=O-1001","resultStatus":"FOUND"}'
```

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/agent-step \
  -H 'Content-Type: application/json' \
  -d '{"stepName":"risk.review","observation":"high amount refund","decision":"require approval"}'
```

## 记录普通事件

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/events \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "stream.disconnect",
    "attributes": {
      "lastEventId": "event-12",
      "reason": "client closed"
    }
  }'
```

## 记录模型成本

```bash
curl -X POST http://localhost:8088/api/traces/{traceId}/model-usage \
  -H 'Content-Type: application/json' \
  -d '{
    "model": "gpt-4o-mini",
    "inputTokens": 100,
    "outputTokens": 200,
    "cost": 0.0003
  }'
```

## 按场景查看成本

```bash
curl http://localhost:8088/api/traces/cost/by-scenario
```

当前 demo 使用内存汇总，生产项目可以换成 Micrometer、日志平台或数据库聚合。

## 用户级和租户级配额

```bash
curl -X POST http://localhost:8088/api/traces/quota/check \
  -H 'Content-Type: application/json' \
  -d '{"tenantId":"tenant-a","userId":"u1001","requestedTokens":500}'
```

默认 `QuotaService` 是内存版，用户配额 20000 token，租户配额 200000 token。

## 记录坏 case 反馈

```bash
curl -X POST http://localhost:8088/api/traces/feedback \
  -H 'Content-Type: application/json' \
  -d '{
    "traceId": "trace-1",
    "scenario": "ticket-advice",
    "rating": "bad",
    "reason": "引用了错误退款制度"
  }'
```

```bash
curl http://localhost:8088/api/traces/quality/ticket-advice
```

## 查询快照

```bash
curl http://localhost:8088/api/traces/{traceId}
```

## 发布验收检查

`ReleaseReadinessChecker` 用代码表达上线前的最小门禁：

- 压测必须通过。
- 安全评审必须通过。
- 灰度计划必须准备好。
- 回滚计划必须准备好。
- 值班入口必须准备好。
- p95 latency、Tool 失败率、坏 case 比例不能超过阈值。

当前 demo 不提供发布平台接口，只通过单元测试验证检查规则。生产项目可以把同样的合同接到发布平台、CI 流程或灰度系统。

## 测试

```bash
mvn -pl ai-observability-demo test
```

正常情况下会看到 15 个测试通过，覆盖：

- Trace、Prompt/RAG/Model/Tool/Agent Span。
- 普通事件记录和 trace 快照查询。
- 模型成本按 trace 和 scenario 汇总。
- 用户级和租户级 token 配额。
- 坏 case 反馈收集和质量报告。
- Controller 结构化接口。
- 发布前压测、安全、灰度、回滚、值班和核心质量指标检查。
