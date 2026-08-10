# 分阶段源码

这里不是最终服务的启动壳，也不是按旧提交复制的历史代码。七个目录都从当前主线实现按能力边界裁剪，前一阶段看不到后一阶段才引入的业务实现。

阶段目录采用累计组合：一个模块在后续阶段没有变化，就直接复用前一阶段的模块，不再复制一份。比如阶段 05 只新增完整的 Ticket Agent、Agent Eval 和 JDK 8 客户端，Knowledge Service 与 Customer BFF 继续使用阶段 03、04 的实现。这样既能单独导入当前阶段的根 `pom.xml`，也能一眼看清本阶段真正增加了什么。

| 阶段 | 对应文章 | 本阶段新增的真实能力 |
| --- | --- | --- |
| `stage-01-system-boundaries` | 01-03 | Knowledge、Ticket Agent、Customer BFF 三个独立进程 |
| `stage-02-model-engineering` | 04-12 | 真实模型调用、身份、结构化输出、SSE、韧性和模型评测 |
| `stage-03-enterprise-rag` | 13-21 | 文档、切分、pgvector、ACL、混合检索、索引任务和检索评测 |
| `stage-04-customer-consultation` | 22-25 | 客户会话、证据展示、反馈、重试和工单接管 |
| `stage-05-controlled-agent` | 26-34 | Planner、Tool、风险分级、人工确认、幂等执行和 JDK 8 客户端 |
| `stage-06-production-readiness` | 35-39 | 在既有业务服务上加入安全回归、生产评测数据、部署配置和发布门禁 |
| `stage-07-framework-boundaries` | 40-51 | Spring AI Alibaba、LangChain4j、AgentScope、MCP 和 A2A 的边界实验 |

学习时直接用 IDEA 打开对应阶段的根 `pom.xml`。需要模型或数据库的阶段统一读取项目根目录唯一的 `config/application-default.yml`，不在阶段之间复制配置。阶段 04-06 会直接导入前面阶段中没有变化的模块，读者不需要手工安装或复制它们。
