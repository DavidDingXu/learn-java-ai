# Release Plan

## Load Test

AI 功能上线前必须单独做压测，不要只复用普通接口压测结果。

压测至少覆盖：

- 普通问答请求。
- RAG 检索请求。
- Agent 多步 Tool 调用请求。
- 流式输出请求。
- 模型超时和 Tool 失败场景。

压测报告要记录：

- p95 latency。
- p99 latency。
- 模型调用失败率。
- Tool 调用失败率。
- Redis、数据库、向量库和线程池资源使用。
- 单请求 token 消耗和场景总成本估算。

当前 readiness 检查里，`maxP95LatencyMs` 超过 2500ms 会阻塞发布，超过 2000ms 会给出预警。

## Security Review

安全检查至少覆盖：

- 用户身份和租户上下文是否进入 Agent 请求。
- RAG 检索是否经过租户和部门权限过滤。
- Tool API 是否有 allowlist、参数校验和审计。
- 写操作 Tool 是否需要人工确认和幂等 token。
- Prompt、RAG 原文、Tool 结果和 Trace 是否避免泄漏敏感信息。
- MCP / A2A 远程端点是否有鉴权、allowlist 和回调校验。

没有完成安全检查时，AI 功能不能进入正式灰度。

## Gray Release

AI 功能上线默认先灰度，不直接全量开放。

灰度顺序：

1. 内部开发账号。
2. 客服小组白名单。
3. 单租户灰度。
4. 多租户按比例放量。

灰度期间必须观察回答质量、工具失败率、人工确认率、token 成本和坏 case 数量。

当前 readiness 检查里，`maxToolFailureRate` 超过 5% 会阻塞发布，超过 4% 会给出预警。

坏 case 比例超过 5% 也会阻塞发布，超过 4% 会给出预警。

## Rollback

回滚策略必须覆盖模型、Prompt、RAG、Tool 和开关。

- 模型失败：切换备用模型或返回非 AI 处理提示。
- Prompt 退化：回滚到上一版本模板。
- RAG 召回异常：关闭 AI 建议，只保留制度检索。
- Tool 异常：禁用对应工具，不影响只读问答。
- 高风险异常：关闭写操作建议入口。

回滚计划必须能在灰度期间执行，不要等全量事故后再补。

## On-call

上线后排障至少需要这些入口：

- trace 查询。
- 模型调用日志。
- RAG 召回和引用详情。
- Tool 调用审计。
- Eval 报告。
- 用户反馈和坏 case 列表。

值班处理要能判断问题归属：模型、Prompt、RAG、Tool、权限、老系统、回调或配额。

## Release Readiness

发布前至少要有这些证据：

- 压测通过。
- 安全评审通过。
- 灰度计划可执行。
- 回滚计划可执行。
- 值班入口可用。
- p95 latency、Tool 失败率、坏 case 比例没有超过阈值。

开源项目里的 `ReleaseReadinessChecker` 用内存对象表达这套门禁。它不是完整发布平台，只负责把上线前必须检查的证据写成可测试的 Java 合同。
