# 阶段 02：模型工程

本阶段在 Knowledge Service 中加入真实模型调用、委托身份、Prompt、结构化输出、SSE、超时重试和观测，并加入模型评测程序。文档上传、向量检索和索引代码尚未出现。

1. 填写项目根目录唯一的 `config/application-default.yml`。
2. 在 IDEA 中运行 `KnowledgeServiceApplication`。
3. 打开 `model-engineering-learning-journey.http`，按篇号运行请求，观察回答、结构化拒答、模型元数据和 SSE。

第 12 篇需要评测时，直接运行 `EvalRunner`，按文章给出的 Program arguments 选择契约数据或真实模型数据。

| 篇目 | 先运行或阅读 | 读完能观察到 |
|---|---|---|
| 04 | HTTP `02-first-model-answer`、`KnowledgeAnswerController` | 第一条真实模型回答 |
| 05 | `KnowledgeIdentityConfiguration` | 服务端身份进入调用链，客户端不传密钥 |
| 06 | `KnowledgeAnswerModel` | 业务端口与框架适配器分离 |
| 07 | `SpringAiKnowledgeAnswerModel` | 模型名、Token、结束原因和 Trace 元数据 |
| 08 | `GroundedPrompt` | 系统约束与证据上下文分层 |
| 09 | HTTP `03-structured-refusal`、`KnowledgeAnswerValidator` | 无依据时得到稳定拒答结构 |
| 10 | HTTP `04-streaming-answer`、`StreamingKnowledgeAnswerService` | SSE 增量事件和最终元数据 |
| 11 | `KnowledgeAnswerConfiguration`、`SpringAiKnowledgeAnswerModel` | 超时、重试与并发准入边界 |
| 12 | `EvalRunner` | 契约数据或真实模型数据生成的评测报告 |

表中没有出现的 `document`、`indexing`、`retrieval` 包属于下一阶段，本阶段无需预读。
