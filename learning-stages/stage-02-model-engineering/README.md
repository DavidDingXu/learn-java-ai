# 阶段 02：模型工程

本阶段在 Knowledge Service 中加入真实模型调用、委托身份、Prompt、结构化输出、SSE、超时重试和观测，并加入模型评测程序。文档上传、向量检索和索引代码尚未出现。

1. 填写项目根目录唯一的 `config/application-default.yml`。
2. 在 IDEA 中运行 `KnowledgeServiceApplication`。
3. 打开 `model-engineering-learning-journey.http`，按篇号运行请求，观察回答、结构化拒答、模型元数据和 SSE。

第 12 篇需要评测时，直接运行 `EvalRunner`，按文章给出的 Program arguments 选择契约数据或真实模型数据。
