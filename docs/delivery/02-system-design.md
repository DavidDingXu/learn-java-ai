# System Design

## Module Boundary

系统拆成 demo 模块和两个主项目。demo 负责单点能力，主项目负责串联完整链路。

- `ai-gateway-demo`：普通 Chat 调用治理、日志和 trace；Advisor / Memory 生产接入优先保留 Spring AI 原生链路。
- `ai-tool-demo`：Tool API 的权限、参数校验、人工确认、幂等和审计；生产接入通过 Spring AI `ToolCallback` 暴露治理后的工具。
- `ai-rag-demo`：文档解析、切分、检索、重排、引用、权限。
- `ai-tool-demo`：Tool API、参数校验、人工确认、审计。
- `ai-agent-demo`：Agent 编排、记忆、上下文工程、Hook。
- `ai-eval-demo`：Golden Set、RAG Eval、Agent Eval、Prompt 回归。
- `ai-observability-demo`：trace、成本、限流、反馈。
- `project-enterprise-rag`：企业制度知识库主项目，承接文档生命周期、权限、引用、索引、评测和 trace。
- `project-helpdesk-agent`：企业工单 AI 助手主项目，承接 Tool API、人工确认、幂等、审计、Agent Eval 和 trace。

## Main Project Strategy

两个主项目按阶段演进，不靠增加换皮业务项目堆复杂度。

| 阶段 | 设计要求 |
|---|---|
| 内存版边界 | 可以使用内存仓储，但权限、引用、确认、幂等、审计、trace 和 eval 必须真实存在 |
| 基础设施替换 | MySQL、Redis、pgvector、MinIO、Elasticsearch 通过仓储或 provider 替换，不改应用服务主流程 |
| 模型接入 | 模型调用通过网关，结构化输出通过解析和修复边界，不允许 Controller 或 Agent 直接依赖厂商 SDK |
| 治理接入 | RAG、Tool、Agent、模型调用都要进入 trace，并能被 Eval 和 bad case 复盘使用 |
| 上线交付 | 需求、设计、接口、评测、发布和回滚计划必须齐全 |

## Data Flow

工单 AI 助手主流程：

```text
用户请求
  -> 老系统或 Spring Boot Controller
  -> Agent Application Service
  -> RAG 检索制度证据
  -> Tool API 查询订单/工单/用户
  -> 结构化输出处理建议
  -> 高风险动作等待人工确认
  -> Trace / Eval / Feedback 记录
```

制度知识库主流程：

```text
文档上传
  -> Parser
  -> Chunker
  -> Embedding
  -> IndexTask
  -> Hybrid Retrieval
  -> Rerank
  -> Evidence Governance
  -> Citation Answer
```

## Trace Point

每次 AI 请求至少记录这些 trace point：

- `model-call`
- `prompt-render`
- `rag-retrieval`
- `rerank`
- `evidence-governance`
- `tool-call`
- `agent-step`
- `human-approval`
- `eval-feedback`

trace 里只记录必要摘要，不落敏感原文和密钥。
