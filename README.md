<p align="center"><a href="https://daviddingxu.github.io/learn-java-ai/"><img src="docs/assets/readme-cover.png" alt="Java AI 应用开发实战" width="100%"></a></p>

<h1 align="center">Java AI 应用开发实战</h1>
<p align="center"><strong>从一次模型调用，走到一条完整业务链。</strong></p>
<p align="center">课程已完结 · 52 篇完整文章 · 免费阅读</p>
<p align="center"><a href="https://daviddingxu.github.io/learn-java-ai/"><strong>在线阅读 →</strong></a> &nbsp; / &nbsp; <a href="#文章目录">文章目录</a> &nbsp; / &nbsp; <a href="https://daviddingxu.github.io/learn-java-ai/downloads/learn-java-ai.pdf">PDF</a> &nbsp; / &nbsp; <a href="https://daviddingxu.github.io/learn-java-ai/downloads/learn-java-ai.epub">EPUB</a></p>

---

以 Java 后端工程师熟悉的客户服务场景为主线，逐步理解模型调用、企业 RAG、受控 Agent，以及它们怎样接入现有业务系统。

适合有 Java / Spring Boot 经验的开发者。52 篇文章已全部公开，以 Spring AI 为主线，并通过独立实验比较其他框架与协议。

**客户咨询 → 带证据的知识回答 → 工单与人工确认 → JDK8 系统执行**

沿着这条链路，学习模型输出的校验、检索前的权限过滤、工具调用的控制，以及评测、安全、观测和发布。全文可独立阅读，配套工程用于进一步验证文章中的实现。

## 文章目录

### 系统边界与第一条调用

- 01 · [专栏介绍：Java 后端怎样系统学习 AI 应用开发落地](docs/course/01-column-introduction.md)
- 02 · [从一次客户咨询看系统分工：知识服务、工单 Agent 与 JDK8 老系统](docs/course/02-system-context.md)
- 03 · [Spring AI 为什么做主线，其他框架放在哪里](docs/course/03-framework-selection.md)
- 04 · [先跑通第一条真实调用：从 ChatClient 到可维护的垂直链路](docs/course/04-first-model-call.md)
- 05 · [多租户、身份和权限为什么不能相信请求参数](docs/course/05-delegated-identity.md)
- 06 · [JDK8 老系统不升级：先划清现代 AI 服务与老系统边界](docs/course/06-jdk8-boundary.md)

### 模型调用工程化

- 07 · [一次模型调用内部发生了什么：Message、Options、Token 与响应元数据](docs/course/07-model-call-metadata.md)
- 08 · [Prompt 的工程边界：模板、版本、授权知识与提示注入](docs/course/08-prompt-boundaries.md)
- 09 · [结构化输出为什么会失败：Schema、DTO 和业务校验](docs/course/09-structured-output.md)
- 10 · [流式输出原理与实现：SSE、取消、心跳、断连和 TTFT](docs/course/10-sse-streaming.md)
- 11 · [超时、并发保护、重试和降级：为什么不需要万能模型网关](docs/course/11-model-resilience.md)
- 12 · [真实模型调用之后：怎样建立评测基线与可观测性](docs/course/12-eval-observation.md)

### 企业 RAG

- 13 · [企业知识库先解决什么：文档、版本、状态和冲突](docs/course/13-document-domain.md)
- 14 · [文档上传链路：对象存储、内容校验、隔离和解析失败](docs/course/14-document-upload.md)
- 15 · [文档切分为什么影响召回：标题、条款、元数据和粒度](docs/course/15-document-chunking.md)
- 16 · [向量检索原理与 pgvector 实现](docs/course/16-pgvector-search.md)
- 17 · [权限为什么必须在 TopK 之前过滤](docs/course/17-acl-before-topk.md)
- 18 · [混合检索、Query Rewrite、Rerank 什么时候有用](docs/course/18-hybrid-retrieval.md)
- 19 · [RAG 回答为什么必须带证据：拒答、冲突、时效与知识注入](docs/course/19-grounded-rag-answer.md)
- 20 · [增量索引为什么需要任务、租约和失败恢复](docs/course/20-incremental-indexing.md)
- 21 · [RAG 质量怎样量化：Golden Set、Recall@K 和坏案例](docs/course/21-rag-evaluation.md)

### 客户咨询与工单升级

- 22 · [C 端为什么需要薄 BFF：鉴权、限流、SSE 和 DTO 聚合](docs/course/22-thin-bff.md)
- 23 · [多轮会话原理：短期状态、裁剪、总结和过期](docs/course/23-conversation-state.md)
- 24 · [用户怎样判断回答可信：证据、反馈、拒答和重试](docs/course/24-answer-trust.md)
- 25 · [机器人解决不了时怎样平滑升级工单](docs/course/25-ticket-handoff.md)

### 受控 Agent 与老系统接入

- 26 · [Agent 与普通聊天有什么区别：目标、状态和受控执行](docs/course/26-agent-boundary.md)
- 27 · [Tool Calling 原理与查询 Tool：最小权限、最小参数和最小返回](docs/course/27-query-tools.md)
- 28 · [为什么模型只能选择工具：参数污染、结果污染和业务规则边界](docs/course/28-tool-trust-boundaries.md)
- 29 · [写操作为什么必须先做风险分级](docs/course/29-action-risk.md)
- 30 · [人工确认为什么不能只传一个字符串 token](docs/course/30-approval-receipt.md)
- 31 · [幂等、并发和审计怎样跨越本地与远程事务边界](docs/course/31-action-consistency.md)
- 32 · [JDK8 老系统接入 AI：OpenAPI、Java 8 客户端、认证、超时与错误处理](docs/course/32-jdk8-integration.md)
- 33 · [串联工单 Agent 业务链：知识查询、人工确认与 JDK8 执行](docs/course/33-ticket-agent-case.md)
- 34 · [Agent 质量怎样评测：工具路径、参数、拒绝和副作用](docs/course/34-agent-evaluation.md)

### 评测、安全与上线治理

- 35 · [AI 安全怎样持续回归：Injection、PII、ACL 与 Tool 越权](docs/course/35-ai-security-regression.md)
- 36 · [观测做到什么程度才够用：Trace、Correlation、Metric、日志和成本](docs/course/36-observability.md)
- 37 · [缓存、熔断、限流和降级：只保留业务真正需要的部分](docs/course/37-production-resilience.md)
- 38 · [项目配置实战：让默认配置跑通，再按需替换基础设施](docs/course/38-runtime-configuration.md)
- 39 · [上线前最后核对什么：测试、Eval、密钥配置、发布和回滚](docs/course/39-release-checklist.md)

### 框架与协议迁移

- 40 · [DashScope Provider 适配实验：配置隔离、响应差异和回归边界](docs/course/40-dashscope-provider-replacement.md)
- 41 · [Embedding 与 Rerank 替换实验：怎样设计同集评测](docs/course/41-domestic-embedding-rerank.md)
- 42 · [Graph 人工确认路由实验：何时比现有状态机更合适](docs/course/42-graph-confirmation-flow.md)
- 43 · [Spring AI Alibaba 兼容性实验：依赖隔离、迁移 ADR 和回归成本](docs/course/43-spring-ai-alibaba-compatibility.md)
- 44 · [LangChain4j AI Services 适配实验：迁移一个低风险用例](docs/course/44-langchain4j-ai-services.md)
- 45 · [LangChain4j RAG 适配实验：复用现有业务契约](docs/course/45-langchain4j-rag-contract.md)
- 46 · [LangChain4j Tool 实验：只读调用与结构化输出](docs/course/46-langchain4j-tool-structured-output.md)
- 47 · [Spring AI 与 LangChain4j 共存实验：按业务能力选择框架](docs/course/47-spring-ai-langchain4j-coexistence.md)
- 48 · [AgentScope Tool 裁决实验：注册、身份传递与权限映射](docs/course/48-agentscope-runtime-contracts.md)
- 49 · [多 Agent 实际协作：并行取证、结果汇总与失败转人工](docs/course/49-multi-agent-human-intervention.md)
- 50 · [MCP 互操作实验：发现、准入、调用与远程失败](docs/course/50-mcp-enterprise-tools.md)
- 51 · [A2A 互操作实验：任务映射、幂等与状态边界](docs/course/51-a2a-task-collaboration.md)
- 52 · [结束语：把 Java + AI 能力迁移到真实公司项目](docs/course/52-column-conclusion.md)

## 配套工程

需要动手验证时，从[七个学习阶段](learning-stages/README.md)选择对应代码，或按[运行指南](docs/getting-started.md)启动完整工程。环境、命令与测试说明均放在各自入口。

## 交流与许可

欢迎 Star 收藏，按需要回查。问题与勘误请提交 [Issue](https://github.com/DavidDingXu/learn-java-ai/issues)，改进内容请参考[贡献指南](CONTRIBUTING.md)。公众号：**好好用 AI**，可在[阅读站](https://daviddingxu.github.io/learn-java-ai/#community)扫码关注。

文章与图示采用 [CC BY-NC-SA 4.0](https://creativecommons.org/licenses/by-nc-sa/4.0/)，代码采用 [PolyForm Noncommercial 1.0.0](LICENSE)。均限非商业使用，详见[许可说明](LICENSING.md)。
