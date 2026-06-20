# ai-output-demo

这个模块演示 AI 结构化输出的 Java 后端边界：严格 JSON 解析、DTO 映射、业务字段校验、坏 JSON 拦截，以及修复 Prompt 构造。

当前实现不会真实调用模型做修复，只负责表达“解析失败后应该把修复重试作为独立步骤”的工程位置。生产环境需要把修复调用、失败降级、Trace、bad case 收集和 Eval 关联起来。

## 核心类

```text
StructuredOutputController
AiJsonParser
TicketAdviceResponse
RiskLevel
```

## 运行测试

在开源项目根目录执行：

```bash
mvn -pl ai-output-demo test
```

正常情况下会看到 7 个测试通过，覆盖：

- 合法工单建议 JSON 解析。
- 缺少风险等级。
- 缺少下一步动作。
- 中高风险建议缺少引用依据。
- 风险等级不是 Java 枚举值。
- 坏 JSON 拦截。
- 修复 Prompt 构造。

## 启动服务

```bash
mvn -pl ai-output-demo spring-boot:run
```

打开前端页面：

```text
http://localhost:8082/
```

页面会把模型 JSON 解析成工单建议卡片，展示摘要、风险等级、下一步动作和引用依据。

解析一段合法 JSON：

```bash
curl -X POST http://localhost:8082/api/output/ticket-advice/parse \
  -H 'Content-Type: application/json' \
  -d '{
    "summary": "客户申请退款，但订单已经发货。",
    "riskLevel": "MEDIUM",
    "nextActions": ["核对物流状态", "查询退款制度"],
    "citations": ["policy-refund-001"]
  }'
```

如果把 `riskLevel` 改成 `"中风险"`，或者把 `nextActions` 改成空数组，接口会因为 DTO 业务校验失败而返回错误。

## 边界

- `AiJsonParser` 当前只做严格解析和修复 Prompt 构造，不直接调用模型。
- `TicketAdviceResponse` 的构造器负责业务字段校验，不把无效建议带进后续流程。
- 结构化输出失败不能只算解析异常，后续应该进入 Trace 和 bad case 样本。
