# ADR 0002：分离主线、labs、Java 8 与前端构建

状态：已接受

日期：2026-07-12

## 背景

仓库包含四个不同的兼容域：

1. Java 21 Spring Boot 服务和 Eval Runner。
2. 拥有独立依赖图的框架迁移实验。
3. 必须在 Java 8 上编译和运行的客户端。
4. Vue/TypeScript 客户端应用。

单一 reactor 会让实验框架依赖泄漏到主服务，迫使 Java 8 客户端继承 Java 21 字节码设置，并把 Node 工具链绑定到 Maven 生命周期。共享领域模块还会鼓励服务直接编译依赖对方的内部模型，而不是维护明确接口。

## 决策

使用四个独立构建边界。

### 主 reactor

根 `pom.xml` 只聚合：

- `services/knowledge-service`
- `services/ticket-agent-service`
- `apps/customer-bff`
- `quality/eval-runner`

该 reactor 使用 `maven.compiler.release=21`，并统一管理 Spring Boot 与 Spring AI 主线版本。

### 框架实验

`labs/pom.xml` 是独立 reactor。每个子模块只导入自己的框架 BOM：

- `labs/spring-ai-alibaba-lab`
- `labs/langchain4j-lab`
- `labs/agentscope-lab`
- `labs/protocol-interop-lab`

labs 不能成为主服务的传递依赖。

### Java 8 客户端

`integrations/jdk8-client/pom.xml` 没有父 POM，并使用 `maven.compiler.release=8`。验证时必须选择同时包含 `java` 和 `javac` 的完整 JDK 8。

### Customer Web

`apps/customer-web` 是独立 Node 产品，不进入 Maven reactor。

服务不共享领域 JAR。跨服务复用只包含 `contracts` 目录中的版本化 OpenAPI、JSON Schema、错误契约和测试 Fixture。

## 影响

收益：

- Java 兼容性可见且可测试。
- 框架实验不会静默改变主服务依赖树。
- 每个产品可以独立验证和发布。
- 服务契约保持显式，不会被共享实现类隐藏。

成本：

- 聚合验证脚本需要运行多个构建。
- 契约变更需要提供方和消费方测试，不能只依赖一次编译错误。
- 生成或手工维护的客户端可能重复部分 DTO 结构。

## 重新评审条件

只有多个产品拥有相同运行时、发布节奏和依赖策略，且合并不会破坏 Java 8 兼容性或框架隔离时，才考虑合并构建边界。

只有稳定技术能力不包含领域所有权，且已经证明 HTTP/OpenAPI 契约或少量重复的成本更高时，才引入共享库。共享领域实体、持久化模型和服务内部 DTO 仍然禁止。
