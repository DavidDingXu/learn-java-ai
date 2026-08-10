# Eval Runner

独立的 AI 应用评测命令行工具。它只通过版本化文件和公开 HTTP API 访问被测系统，不依赖服务实现类，不读取业务数据库。

## 支持的任务

- `contract-validate`：检查 OpenAPI、JSON Schema 和正反例 Fixture。
- `contract-eval`：使用进程内确定性 HTTP Fixture 验证模型交互评测流程。
- `model-eval`：通过 Knowledge Service 公开接口运行真实模型 Golden Set。
- `retrieval-eval`：计算 Recall@K、HitRate@K、MRR、重复率和 p95。
- `agent-eval` 与 `security-eval`：通过公开 HTTP 验证 Agent 路径；本地固定身份无需令牌，JWT 环境使用分离的创建、运行和读取令牌。

## 运行

在 IDEA 中打开 `EvalRunner`，将文章给出的参数填入 Run Configuration 的 Program arguments 后直接运行。报告写入 `var/reports`，读者不需要先执行 Maven 或单元测试。完整参数、令牌文件和报告边界见[模型、检索与 Agent 评测](../../../../docs/runbooks/model-interaction-eval.md)。

本地先用固定身份跑通报告链路；连接受保护环境时，必须使用专用测试租户和最小权限短时令牌。报告可以记录数据集、模型、Prompt、代码版本、延迟和 Trace ID，不得记录 API Key、Bearer Token、内网地址或未脱敏数据。
