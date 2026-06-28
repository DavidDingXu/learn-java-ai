# Java + AI 工程化落地实战路线图

## 项目定位

面向 Java 后端工程师，围绕一个完整工程体系讲清楚 AI 应用如何从 Demo 走向生产：模型调用、流式输出、Prompt、结构化输出、RAG、Agent、Tool API、老系统接入、MCP、A2A、Eval、全链路追踪和生产治理。

这条路线默认优先使用框架底层能力：Spring AI 的 `ChatClient`、`PromptTemplate`、结构化输出、Tool、Advisor、Memory、Embedding、VectorStore 和 MCP 能力不重新实现。项目代码重点补上层工程治理：权限、审计、Trace、Eval、成本、限流、灰度、回滚、坏 case 复盘和生产替换点。

学习顺序按“治理语义先行，框架扩展点承接”组织：先用 `ai-gateway-demo` 把普通 Chat 调用里的路由、超时、重试、降级、日志和 trace 讲清楚；再在结构化输出、Tool、Advisor、Memory、RAG 等模块里保留 Spring AI 原生能力，并把同类治理要求挂到各自链路上。

## 两条主项目线

学习路线不是只看 demo。demo 模块负责讲单点机制，两个主项目负责串完整业务链路。

| 主项目 | 承接专题 | 学习重点 |
|---|---|---|
| `project-enterprise-rag` | 企业 RAG、Eval、Trace、生产治理 | 文档生命周期、权限、引用、索引、知识治理、RAG 评测 |
| `project-helpdesk-agent` | Agent、Tool API、老系统接入、Eval、Trace | 工单上下文、受控 Tool、人工确认、幂等、审计、Agent 评测 |

建议先跑通 demo，再回到主项目看同一能力在业务链路中的位置。完整说明见 [main-project-roadmap.md](main-project-roadmap.md)。

## 9 个工程专题

| 篇章 | 主题 | 目标 |
|---|---|---|
| 01 | 框架选型与工程骨架 | 选清主框架，搭起可维护 Maven 多模块工程 |
| 02 | 模型调用工程化 | 普通 Chat 调用治理、路由、重试、降级、成本日志 |
| 03 | 流式交互工程化 | 实现 SSE、心跳、断连续传、TTFT 指标 |
| 04 | Prompt 与结构化输出 | 使用 Spring AI 模板和结构化输出能力，补版本、回滚、业务校验和失败契约 |
| 05 | 企业 RAG 知识库 | 文档解析、切分、检索、重排、引用、权限 |
| 06 | Agent 与 Tool API | 让 Agent 受控调用 Java 业务能力 |
| 07 | 老 Java 系统接入 AI | JDK8 老系统通过 API 接入外部 Agent 服务 |
| 08 | MCP / A2A / Agent Skill | 用协议标准化工具和 Agent 能力 |
| 09 | Eval、追踪与生产治理 | 评测、回归、trace、成本、限流、坏 case 复盘 |

## 模块学习路线

| 序号 | 主题 | 代码模块 | 动手练习 |
|---|---|---|---|
| 00 | Java 后端做 AI 工程化，到底要学什么 | docs | 画出自己业务系统接入 AI 的模块图 |
| 01 | Spring AI、Spring AI Alibaba、LangChain4j、AgentScope 怎么选 | docs/framework-choice.md | 给自己的项目写选型说明，不新增代码模块 |
| 02 | 从 0 搭建 Java AI 工程化开源项目 | ai-common | 跑通 Maven 多模块构建 |
| 03 | 不要把大模型调用写进 Controller | ai-gateway-demo | 为普通 Chat 调用增加备用模型 Provider |
| 04 | 模型调用的超时、重试、降级怎么做 | ai-gateway-demo | 主模型失败后自动降级 |
| 05 | AI 调用日志要比普通接口多记什么 | ai-gateway-demo | 记录 model、latency、traceId |
| 06 | 流式输出不是返回 Flux 这么简单 | ai-streaming-demo | 实现 SSE token 输出 |
| 07 | SSE 断连后怎么恢复 | ai-streaming-demo | 支持 Last-Event-ID |
| 08 | 流式接口怎么记录首 token 时间 | ai-streaming-demo | 输出 TTFT 指标 |
| 09 | Prompt 不要散落在 Java 字符串里 | ai-prompt-demo | 实现 PromptTemplateRepository |
| 10 | Prompt 版本管理与回滚 | ai-prompt-demo | 增加版本发布表 |
| 11 | Prompt Injection 在业务系统里怎么发生 | ai-prompt-demo | 实现输入风险检测 |
| 12 | AI 输出 JSON 老是坏，Java 后端怎么兜底 | ai-output-demo | 验证坏输出返回 400，并设计 trace / bad case 入口 |
| 13 | 结构化输出实战：生成工单处理建议 | ai-output-demo | 使用 Spring AI `BeanOutputConverter` 输出 TicketAdviceResponse |
| 14 | 结构化输出的单元测试怎么写 | ai-output-demo | 覆盖 Spring AI schema、业务校验和坏 JSON 测试 |
| 15 | RAG 不是把文档塞进向量库 | ai-rag-demo | 上传制度文档并建立索引任务 |
| 16 | PDF、Word、Markdown 怎么解析 | ai-rag-demo | 实现 DocumentParser SPI |
| 17 | 文档切分怎么影响回答质量 | ai-rag-demo | 实现标题感知切分 |
| 18 | Metadata 怎么设计才方便检索 | ai-rag-demo | 增加 tenant、dept、docType 字段 |
| 19 | Embedding 批处理和缓存 | ai-rag-demo | 实现批量向量化 |
| 20 | 向量库选型：ES、pgvector、Milvus 怎么取舍 | project-enterprise-rag | 设计 chunk 索引替换点，保留权限和引用边界 |
| 21 | 混合检索怎么写 | project-enterprise-rag | 跑通当前混合检索测试，再设计 BM25 / Vector / RRF 扩展点 |
| 22 | Reranker 为什么能救很多 RAG 问题 | project-enterprise-rag | 在当前检索结果后增加 reranker provider 边界 |
| 23 | RAG 回答必须带引用 | project-enterprise-rag | 答案返回 sourceId 和 chunkId |
| 24 | 企业知识库必须先做权限 | project-enterprise-rag | 实现 DocumentAccessFilter |
| 25 | 文档增量更新和重建索引 | project-enterprise-rag | 跑通版本替换和索引任务测试，再设计持久化替换点 |
| 26 | RAG 质量怎么评估 | project-enterprise-rag | 跑通 retrieval hit 和 citation hit 评测 |
| 27 | Agent 和普通工作流有什么区别 | ai-agent-demo | 实现确定性工作流版本 |
| 28 | ReAct 不要神化 | ai-agent-demo | 实现受限步骤 Agent |
| 29 | Tool API 为什么不能等于业务 Service | ai-tool-demo / project-helpdesk-agent | 设计 TicketToolFacade |
| 30 | 查询类 Tool 怎么写 | ai-tool-demo / project-helpdesk-agent | 实现 TicketLookupTool |
| 31 | 写操作 Tool 为什么必须二次确认 | ai-tool-demo / project-helpdesk-agent | 实现 ConfirmableAction |
| 32 | Tool 参数校验怎么做 | ai-tool-demo / project-helpdesk-agent | 实现 ToolParameterValidator |
| 33 | Tool 调用审计怎么落库 | ai-tool-demo / project-helpdesk-agent | 实现 ToolExecutionLedger |
| 34 | Agent 记忆不能等于聊天记录 | ai-agent-demo / project-helpdesk-agent | 实现 ConversationMemoryStore |
| 35 | 多轮工单处理上下文怎么维护 | ai-agent-demo / project-helpdesk-agent | 实现 session scope |
| 36 | 工单 AI 助手端到端实战 | project-helpdesk-agent | 查工单、查订单、检索制度、给建议 |
| 37 | 老 JDK8 系统怎么接 Agent 服务 | ai-legacy-demo | 拆出 AgentTask API、Tool API 和 OperatorContext |
| 38 | 老系统调 Agent，Agent 调老系统 Tool API 算循环依赖吗 | ai-legacy-demo | 拆分 AgentTask API 和 Tool API |
| 39 | 老系统权限怎么传给 Agent | ai-legacy-demo | 实现 OperatorContext |
| 40 | MCP 到底解决什么问题 | ai-mcp-demo | 写一个只读制度中心 MCP Server |
| 41 | Spring Boot 如何接入 MCP Client | ai-mcp-demo | 自动发现工具 |
| 42 | MCP 工具权限边界怎么设计 | ai-mcp-demo | 实现工具 allowlist |
| 43 | A2A 和 Agent Skill 适合什么场景 | ai-a2a-demo | 暴露 Agent Card、Skill 和 Task |
| 44 | 用 Java 暴露一个工单 Agent Skill | ai-a2a-demo | 实现任务状态、事件流和回调 |
| 45 | AI 功能上线前必须有 Eval | ai-eval-demo | 定义 Golden Set |
| 46 | RAG 评测：召回对了不等于答对 | ai-eval-demo | retrieval eval + citation eval |
| 47 | Agent 评测：工具调用路径怎么验收 | ai-eval-demo / project-helpdesk-agent | 评估 step accuracy |
| 48 | LLM-as-Judge 不能盲信 | ai-eval-demo | 增加人工抽检字段 |
| 49 | Prompt 改动为什么要回归测试 | ai-eval-demo | 实现 RegressionRunner |
| 50 | AI 请求为什么必须做全链路追踪 | ai-observability-demo / 两个主项目 | 定义 AiTrace、AiSpan、ModelUsage |
| 51 | Prompt、RAG、Tool、Agent 怎么进入 Trace | ai-observability-demo / 两个主项目 | 串联 trace 上下文 |
| 52 | 成本监控：Token 账单突然翻倍怎么发现 | ai-observability-demo | 按场景汇总 token 和成本 |
| 53 | 用户级和租户级限流怎么做 | ai-observability-demo | 实现 quota |
| 54 | 线上坏 case 怎么收集和复盘 | ai-observability-demo / 两个主项目 | 把 trace、eval case 和人工反馈串成复盘样例 |
| 55 | 项目实战总装：企业制度 RAG 知识库 | project-enterprise-rag | 跑完整知识库链路 |
| 56 | 项目实战总装：企业工单 AI 助手 | project-helpdesk-agent | 跑完整 Agent 链路 |
| 57 | Context Engineering：上下文不是把历史消息全塞进 Prompt | ai-agent-demo | 实现上下文切片、预算控制和来源标记 |
| 58 | 会话历史怎么存、怎么裁剪、怎么压缩 | ai-agent-demo | 对比会话窗口、业务快照、长期偏好和摘要切片 |
| 59 | Harness：Prompt、RAG、模型策略怎么做 A/B 和回归 | ai-eval-demo | 对比 baseline 和 candidate 的质量、成本、延迟 |
| 60 | Spring AI Advisor 和 Memory 在工程链路里的位置 | ai-agent-demo | 用 `SpringAiAdvisorMemoryBridge` 对齐 Spring AI 原生 Advisor / Memory，业务侧只补上下文范围、预算、失败策略和 trace 合同 |
| 61 | Spring AI Alibaba Graph：什么时候需要 StateGraph | docs/framework-choice.md / ai-agent-demo | 判断复杂编排是否需要 Graph，先复用受控 Agent 边界 |
| 62 | Agent Hook 和 Interceptor：PII、工具限制和上下文编辑 | ai-agent-demo | 在 ReAct 动作执行前加入脱敏、白名单和 hookEvents |
| 63 | Tool 调用的异步、缓存、线程池和大结果驱逐 | ai-tool-demo | 实现只读缓存、超时失败和大结果裁剪 |
| 64 | RAG Query Rewrite、多路查询和上下文压缩 | ai-rag-demo / project-enterprise-rag | 实现 query 改写、多路检索合并和 citation 安全压缩 |
| 65 | 企业知识冲突和时效性治理 | project-enterprise-rag | 实现证据时效、冲突和优先级治理 |
| 66 | MCP / A2A 远程传输、调试和回调 | ai-mcp-demo / ai-a2a-demo | 实现 MCP 远程端点调试、A2A input-required 和 push callback 合同 |
| 67 | SPEC / SDD：AI 项目上线前要交付哪些设计文档 | docs/delivery / 两个主项目 | 建立需求、设计、接口、评测和发布计划 |
| 68 | 上线前压测、安全检查、灰度和发布验收 | ai-observability-demo / docs/delivery / 两个主项目 | 实现发布前压测、安全、灰度、回滚和值班 readiness 检查 |
| 69 | 写在最后：Java + AI 工程化真正要带走什么 | 全项目 | 按普通 Chat 治理、Prompt、RAG、Tool、Eval、Trace、发布证据复盘迁移路径 |

## 扩展路线

当前主题覆盖了 Java + AI 工程化落地的主干。后续可以继续沿两个方向扩展：一类是补更多真实业务场景，一类是按团队内部平台能力拆出专门模块，例如模型调用治理台、评测样本管理、知识库运营台和 Trace 查询台。
