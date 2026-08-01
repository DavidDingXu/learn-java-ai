# 贡献指南

感谢你对 Java AI Engineering Practice 的关注。项目接受缺陷修复、测试补充、文档改进和边界清晰的小型能力扩展。重大架构变更请先发起 Discussion 或 Feature Request，避免实现方向与项目边界冲突。

## 开发环境

- JDK 21 或更高版本，必须同时包含 `java` 和 `javac`。
- 修改 `integrations/jdk8-client` 时需要完整 JDK 8。
- 修改 `apps/customer-web` 或 Node 契约时需要 Node.js 24。
- 项目自带 Maven Wrapper，不要求贡献者额外安装 Maven。

从主分支创建短期分支，完成改动后运行与范围对应的验证。

## 项目边界

- 主 reactor 只包含 Knowledge Service、Ticket Agent Service、Customer BFF 和 Eval Runner。
- Spring AI Alibaba、LangChain4j、AgentScope 与协议实验留在 `labs` 独立 reactor。
- Java 8 客户端只依赖 HTTP/OpenAPI 和 JSON，不引入 Spring AI，不与 Java 21 服务共享 DTO。
- 服务之间不共享领域 JAR，不跨服务访问数据库。
- 写 Tool 不得绕过权限、参数校验、风险分级、人工确认、幂等和审计。
- 不为了演示额外引入消息中间件、共享基础模块或无调用方的抽象。

修改架构边界时，必须同步更新 ADR、OpenAPI、文档和约束测试。

## 实现与测试

行为变更先补能够复现问题或表达期望的测试，再修改实现。测试优先锁定稳定业务行为和失败分支，不与私有方法或框架内部细节强绑定。

常用验证命令：

```bash
# Java 21 主应用
./mvnw verify

# 独立框架与协议实验
./mvnw -f labs/pom.xml verify

# 完整仓库（还需 JDK 8 和 Node.js 24）
scripts/verify-unit.sh
```

Windows 使用 `.\mvnw.cmd` 与 `.\scripts\verify-unit.ps1`。无法运行某项检查时，在 Pull Request 中写明未运行的命令和原因。

## 配置与密钥

仓库只允许提交不可用的占位值。本地真实模型测试使用已被 Git 忽略的 `config/application.yml`。不要把 API Key、Bearer Token、内网地址、用户数据或私有 Provider 详情写入代码、Fixture、日志、截图和测试报告。

如果发现密钥泄露，先撤销和轮换，再按 [SECURITY.md](SECURITY.md) 私密报告；仅从最新提交删除不能消除历史泄露。

## Pull Request 检查项

- 改动聚焦一个问题，没有无关重构或格式化噪音。
- 新行为或缺陷有对应的回归测试。
- 配置、接口、数据或架构变更已同步文档。
- macOS/Linux 与 Windows 入口保持等价，或已说明平台限制。
- 未提交密钥、令牌、内网地址、客户数据或本地 IDE 文件。
- Pull Request 描述列出已运行命令和关键结果。

提交 Pull Request 即表示同意以本项目的 [Apache License 2.0](LICENSE) 提供所提交的内容。
