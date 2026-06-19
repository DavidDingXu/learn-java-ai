# 两个主项目学习路线

本仓库有很多 demo 模块。demo 负责拆单点能力，两个主项目负责把能力放进完整业务链路。

两个主项目分别对应 Java + AI 落地里很常见的两类系统：

| 主项目 | 业务问题 | 工程重点 |
|---|---|---|
| `project-enterprise-rag` | 企业制度知识库怎么回答得有依据 | 文档生命周期、权限、引用、检索质量、知识治理、RAG Trace |
| `project-helpdesk-agent` | 工单 AI 助手怎么执行得受控 | Tool API、人工确认、幂等、审计、Agent Trace、Agent Eval |

学习时可以先在 demo 模块里看清机制，再回到主项目里看完整链路。

## 阶段 1：先跑通内存版边界

这一阶段不需要 MySQL、Redis、pgvector、MinIO，也不需要真实模型 API。目标是先看清楚对象边界和流程。

运行企业制度 RAG：

```bash
mvn -pl project-enterprise-rag test
```

重点看：

```text
EnterpriseRagProjectTest
EnterpriseRagControllerTest
EvidenceGovernanceServiceTest
```

当前已经覆盖：

- 文档上传和版本。
- chunk 生成和旧版本替换。
- 租户和部门权限过滤。
- 混合检索。
- 引用回答。
- 证据冲突和时效治理。
- RAG Eval。
- RAG Trace。

运行工单 AI 助手：

```bash
mvn -pl project-helpdesk-agent test
```

重点看：

```text
HelpdeskAgentProjectTest
HelpdeskAgentControllerTest
```

当前已经覆盖：

- 查工单、查订单、检索制度。
- 根据订单状态和制度生成处理建议。
- 高风险动作要求人工确认。
- 关闭工单要求 confirmation token。
- 重复 token 幂等处理。
- 已关闭工单不能换 token 重复关闭。
- Tool 调用审计。
- Agent Trace。
- Agent Eval。

## 阶段 2：把单点 demo 对应回主项目

学每个 demo 时，可以顺手看一个问题：这个能力在主项目里落到哪里？

| demo 模块 | 主项目落点 |
|---|---|
| `ai-gateway-demo` | `project-helpdesk-agent` 后续生成建议时通过模型网关调用模型 |
| `ai-prompt-demo` | 工单建议、RAG 回答、评测判分都需要 Prompt 模板和版本 |
| `ai-output-demo` | 工单建议不能只靠字符串，要解析成 `AgentAdviceResult` 这类结构化结果 |
| `ai-rag-demo` | `project-enterprise-rag` 的权限、引用、检索、索引任务 |
| `ai-tool-demo` | `project-helpdesk-agent` 的 `TicketToolFacade` 和 `ToolExecutionLedger` |
| `ai-agent-demo` | 工单助手的受控编排、上下文、记忆和 hook |
| `ai-eval-demo` | 两个主项目的 RAG Eval、Agent Eval、Prompt 回归 |
| `ai-observability-demo` | 两个主项目的 trace、成本、限流和坏 case |

## 阶段 3：替换基础设施

当前主项目用内存仓储表达边界。替换基础设施时，业务流程保持不变，只替换仓储和外部服务实现。

`project-enterprise-rag` 推荐替换顺序：

1. `PolicyDocument` 元数据进入 MySQL。
2. 原始文件进入 MinIO。
3. `DocumentChunk` 索引进入 pgvector 或 Elasticsearch。
4. 索引任务状态持久化。
5. embedding 和 reranker 通过 provider 接口调用真实模型服务。

`project-helpdesk-agent` 推荐替换顺序：

1. 工单、订单、审计记录进入 MySQL。
2. 会话上下文、确认 token、短期缓存进入 Redis。
3. 制度检索调用 `project-enterprise-rag` 的 REST API。
4. 建议生成调用 `ai-gateway-demo` 暴露的模型网关能力。
5. 结构化输出校验复用 `ai-output-demo` 的修复和回归思路。

## 阶段 4：接入治理能力

AI 项目上线后，除了“能不能回答”，还要持续处理质量退化、成本异常、权限误召回、工具误调用和坏 case 复盘。

两个主项目都要逐步接入这些能力：

- Golden Set。
- Prompt / RAG / Tool 回归测试。
- traceId 贯穿模型、RAG、Tool、Agent。
- 质量、延迟、成本统计。
- 用户反馈和坏 case 复盘。
- 灰度、回滚和发布门禁。

## 阶段 5：形成上线交付物

仓库里的 `docs/delivery/` 是最小交付文档样例：

```text
01-requirements-spec.md
02-system-design.md
03-api-contract.md
04-eval-plan.md
05-release-plan.md
```

上线前至少要能回答这些问题：

- 需求边界是什么，哪些事情 Agent 不能做。
- 系统边界在哪里，哪些能力归老系统，哪些能力归 AI 服务。
- REST API、Tool API、RAG API 的输入输出是什么。
- 评测集覆盖哪些典型场景和风险场景。
- 发布失败怎么回滚，坏 case 怎么进入复盘。

交付文档要和真实项目一起评审。先阅读 `docs/delivery/`，再跑两个主项目的回归测试，确认需求、设计、接口、评测和发布计划没有脱离代码边界。
