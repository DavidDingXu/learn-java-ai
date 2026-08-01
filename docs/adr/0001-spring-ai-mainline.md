# ADR 0001：使用 Spring AI 作为主线框架

状态：已接受

日期：2026-07-13

## 背景

目标开发团队已经使用 Spring Boot。主业务链路需要模型调用、结构化输出、流式响应、RAG、Tool Calling、可观测和 MCP，但不应因为加入 AI 能力就替换整个 Java 应用架构。

项目同时评估 Spring AI Alibaba、LangChain4j 和 AgentScope。如果把四套框架都放入主 reactor，依赖冲突、运行时行为和所有权都会变得难以判断。只有保持业务接口、规则和数据集不变，框架比较才有意义。

直接使用 Provider SDK 也在候选范围内。它能更早使用供应商特有能力，但会把认证、配置、重试、元数据映射和迁移成本推入业务代码。

## 决策

Java 21 主服务使用 Spring Boot 4.1.x 与 Spring AI 2.0.x。

Spring AI 2.0.0 是 GA 版本。它的 Boot 4 基线、统一 Tool Calling Advisor、不可变 Options 与结构化输出校验方向与当前工程一致。现有 Spring Boot 3 应用在平台升级获批前继续使用 Spring AI 1.1.x 维护线，通过版本化 HTTP 契约与本系统集成，不共享框架类。

- Spring AI 类型只保留在基础设施适配器。
- 应用端口使用知识回答、工单规划等业务语义。
- 不建设通用模型网关，也不把 Chat、RAG、Tool 与 Agent 简化成 `String -> String`。
- 只有 Spring AI 无法暴露必要能力时，才在专用适配器后使用 Provider SDK，并记录能力差距。
- Spring AI Alibaba、LangChain4j 和 AgentScope 保留在独立 `labs` reactor。候选实现使用相同业务接口和评测规则；生产比较还必须使用相同数据集与目标环境。

## 影响

收益：

- Spring Boot 团队可以继续使用熟悉的配置、依赖管理、测试和可观测方式。
- 主服务保持一套一致的应用架构，不为每个框架切换组织方式。
- 迁移实验可以比较真实的代码、行为和维护成本。

成本：

- 部分 Provider 特有能力可能晚于官方 SDK。
- Spring AI 升级必须同时检查 Spring Boot 与目标 Provider 的兼容性。
- 业务端口和框架适配器比在 Controller 中直接调客户端需要更明确的边界维护。

## 重新评审条件

出现以下任一条件时，重新评审主线选择：

- 必需的 Provider 能力无法通过 Spring AI 或窄适配器实现，或者会丢失关键行为。
- 公司已经运行受治理的 AI 平台，且平台契约是强制集成边界。
- 其他 Java 框架在相同业务接口和数据集上产生可重复的质量或维护成本优势，且迁移成本可接受。
- 当前 Spring Boot、Spring AI 和 Provider 组合不再满足部署环境的安全或兼容支持要求。

更换主线必须新增 ADR，并提供依赖树、回归结果和回滚路径。流行度或 API 偏好不足以触发更换。
