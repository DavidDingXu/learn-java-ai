# 版本基线

锁定日期：2026-08-10

以下版本是仓库的兼容性基线，不表示所有公司项目都应使用同样的版本。

| 范围 | 版本 | 依据 |
|---|---:|---|
| Main Java bytecode | 21 | root `pom.xml` |
| Spring Boot | 4.1.0 | root `pom.xml` |
| Spring AI BOM | 2.0.0 | root `pom.xml` |
| Maven Compiler Plugin | 3.15.0 | root and independent POMs |
| Maven Enforcer Plugin | 3.6.3 | root and independent POMs |
| Maven Surefire/Failsafe | 3.5.6 | root and independent POMs |
| JaCoCo | 0.8.15 | root `pom.xml` |
| ArchUnit | 1.4.2 | root `pom.xml` |
| Maven Wrapper | 3.9.14 | `.mvn/wrapper/maven-wrapper.properties` |
| Spring AI Alibaba BOM | 1.1.2.3 | `labs/pom.xml` |
| LangChain4j BOM | 1.18.1 | `labs/pom.xml` |
| AgentScope BOM | 2.0.2 | `labs/pom.xml` |
| MCP Java SDK | 2.0.0 | `labs/pom.xml` and `labs/protocol-interop-lab/pom.xml` |
| A2A Java SDK | 1.2.0.Final | `labs/pom.xml` and `labs/protocol-interop-lab/pom.xml` |
| Tomcat Embed | 11.0.24 | `labs/pom.xml` |
| SnakeYAML | 2.6 | `labs/pom.xml` |
| Labs JUnit | 6.0.3 | `labs/pom.xml` |
| Legacy Java target | 8 | `integrations/jdk8-client/pom.xml` |
| Legacy JUnit | 5.11.4 | `integrations/jdk8-client/pom.xml` |
| PostgreSQL local RAG baseline | 17.10 | `docs/runbooks/rag-prerequisites.md` local verification |
| pgvector local RAG baseline | 0.8.6 | `docs/runbooks/rag-prerequisites.md` local verification |
| Ollama fallback baseline | 0.32.6 | `docs/runbooks/rag-prerequisites.md` local verification |

## 维护规则

- 主服务从根 POM 的依赖与插件管理继承版本；没有兼容性理由时，子 POM 不重复声明。
- 每个 labs 子模块只导入自己的框架 BOM，labs 依赖不能进入主 reactor。
- Java 8 客户端维护独立插件与测试版本，不继承 Java 21 构建配置。
- 只有已存在对应垂直用例和验证计划时，才增加 Provider、数据库、向量库和对象存储依赖。
- 补丁版本升级需要单元、契约和适用的真实环境验证；主版本升级还需要记录依赖、行为差异和回滚条件的 ADR。
- CI 在真实 JDK 21 JVM 上运行主 reactor。在 JDK 26 上使用 `--release 21` 只能验证字节码兼容，不等价于 JDK 21 运行时结果。
- 优先使用稳定 GA 版本，不因 Maven metadata 中的版本号更大就选择 milestone 或 release candidate。
- 表中框架与 SDK 版本在锁定日期可从 Maven Central 解析，并已通过 labs reactor 构建。
