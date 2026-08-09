# 七个阶段的可运行学习切片

主项目保存完整业务实现，`learning-stages` 则把专栏的七个阶段拆成独立 module。读到哪一段，只打开对应 module，运行其 `Application` 类。

这些 module 不复制七份业务系统。它们是小型、可执行的学习切片，只组织当前阶段需要的输入、状态和可观察结果；真正的生产实现仍只保留在 `services`、`apps` 和 `labs` 中。

## 配置

模型 API Key 继续填在项目根目录 `config/application.yml`。阶段应用需要的服务地址和固定数据路径位于 `learning-stages/config/application.yml`，不需要配置环境变量。

IDE 的 Working directory 统一设为项目根目录。

如果 IntelliJ 没有自动识别这些 module，在 Maven 工具窗口选择 `Add Maven Projects`，打开 `learning-stages/pom.xml`。这一步只让 IDE 导入源码和依赖；导入完成后仍然直接运行表格中的 `Application`，不需要在终端执行构建或测试命令。

## 阶段入口

| 篇目 | module | 直接运行 | 可观察结果 |
|---|---|---|---|
| 01-03 | `stage-01-system-boundaries` | `SystemBoundariesStageApplication` | 四个系统各自拥有什么业务事实 |
| 04-12 | `stage-02-model-engineering` | `ModelEngineeringStageApplication` | 真实回答、模型名、引用与拒答状态 |
| 13-21 | `stage-03-enterprise-rag` | `EnterpriseRagStageApplication`，启动参数填当前篇号 | 上传、发布、索引、向量/混合对比、ACL 负例与评测报告 |
| 22-25 | `stage-04-customer-consultation` | `CustomerConsultationStageApplication` | 会话 ID、回答尝试、引用和后续升级入口 |
| 26-34 | `stage-05-controlled-agent` | `ControlledAgentStageApplication` | 任务状态、运行结果或待确认动作 |
| 35-39 | `stage-06-production-readiness` | `ProductionReadinessStageApplication` | 三个服务健康状态和实际暴露的指标 |
| 40-51 | `stage-07-framework-boundaries` | `FrameworkBoundariesStageApplication` | 每组框架实验真正回答的迁移问题 |

## RAG 阶段的一次性设置

在 Knowledge Service 的 `application.yml` 中使用：

```yaml
java-ai:
  knowledge:
    mode: postgres-rag
    embedding:
      mode: ollama
      ollama:
        base-url: http://localhost:11434
        model: qwen3-embedding:4b
        timeout-seconds: 120
    retrieval:
      mode: hybrid
      lexical-search: true
      rewrite-query: false
      rerank: false
    indexing:
      scheduler-enabled: false
```

先在 Ollama 应用中下载 `qwen3-embedding:4b`。配好 PostgreSQL 并保持 Ollama 运行后，运行 `KnowledgeServiceApplication`。阅读第 13-21 篇时运行 `EnterpriseRagStageApplication`，把当前篇号填入 IDE 的 Program arguments；填写 `all` 或留空则一次运行完整链路。

| 启动参数 | 可观察结果 |
|---|---|
| `13` | 查看文档身份、生命周期与发布 ACL |
| `14` | 上传可读政策与 ACL 负例文档 |
| `15` | 发布两个版本，生成索引任务并写入 Chunk 与向量 |
| `16` | 查看 pgvector TopK |
| `17` | 确认财务文档没有越权进入 TopK |
| `18` | 对比向量与混合检索结果 |
| `19` | 查看带引用的回答 |
| `20` | 再次运行 Worker，观察没有重复任务 |
| `21` | 生成 `var/learning-stage-reports/rag-learning-journey.md` |

报告会记录本次使用的 Embedding 模型。`qwen3-embedding:4b` 的结果可以作为本地语义检索基线；换到目标环境时，仍需使用目标数据和目标模型重新评测。`local-hash` 只检查文档、任务、pgvector、ACL、引用和 Chat 是否连通，不能当作质量基线。

阶段进度保存在 `var/learning-stage-reports/rag-stage-state.json`。同一篇可以重复运行，已经完成的上传、发布和索引步骤会直接复用；后续篇目若缺少前置状态，程序会明确提示先运行哪个篇号。更换为全新的专用数据库时，同时删除旧状态文件，再从 `13` 开始。
