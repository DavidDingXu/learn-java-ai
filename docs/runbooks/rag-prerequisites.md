# RAG 本地准备：PostgreSQL、pgvector 与 Embedding

阶段 03 的主路径只使用项目根目录一份本地配置：`config/application-default.yml`。Chat 与 Embedding 默认走同一个 OpenAI 兼容地址和 API Key，只需分别填写两个模型名。只有该接口不提供 Embedding 时，才改用文末的 Ollama 方案。

本仓库在 macOS 上核对过 PostgreSQL `17.10`、pgvector `0.8.6` 和 Ollama `0.32.6`。版本不必逐字相同，但 PostgreSQL 应保持在 17.x 的受支持补丁版本，pgvector 必须能为该实例创建 `vector` 扩展。

## 1. 创建唯一的本地配置

复制根目录的 `config/application-default.example.yml` 为 `config/application-default.yml`，只编辑副本。副本已被 `.gitignore` 忽略，所有阶段都会自动读取它。

先保持 Chat 与 Embedding 都使用 `openai`：

```yaml
spring:
  ai:
    model:
      chat: openai
      embedding: openai
    openai:
      api-key: replace-with-your-api-key
      base-url: https://api.openai.com/v1
      chat:
        model: gpt-4.1-mini
      embedding:
        model: text-embedding-3-small
```

如果同一个兼容接口不能调用 Embedding，索引请求会明确失败；此时再使用文末的 Ollama 配置，不必修改 Java 代码。

## 2. macOS 安装并手动启停 PostgreSQL

Homebrew 安装命令：

```bash
brew install postgresql@17 pgvector
```

不要执行 `brew services start`。需要 RAG 时手动启动，完成后手动停止：

```bash
"$(brew --prefix postgresql@17)/bin/pg_ctl" \
  -D "$(brew --prefix)/var/postgresql@17" \
  -l "$(brew --prefix)/var/postgresql@17/server.log" start

"$(brew --prefix postgresql@17)/bin/pg_ctl" \
  -D "$(brew --prefix)/var/postgresql@17" stop
```

若数据目录尚未初始化，先执行一次：

```bash
"$(brew --prefix postgresql@17)/bin/initdb" \
  -D "$(brew --prefix)/var/postgresql@17"
```

## 3. Windows 安装并设为手动启动

从 [PostgreSQL Windows 下载页](https://www.postgresql.org/download/windows/) 安装 PostgreSQL 17。安装完成后打开“服务”，找到 `postgresql-x64-17`，把启动类型改为“手动”。需要联调时再用管理员 PowerShell 启停：

```powershell
Start-Service postgresql-x64-17
Stop-Service postgresql-x64-17
```

pgvector 的 Windows 安装需要 Visual Studio 的“使用 C++ 的桌面开发”组件，并在 x64 Native Tools Command Prompt 中按[官方 Windows 安装步骤](https://github.com/pgvector/pgvector#windows)构建到 PostgreSQL 17 目录。不要把 PostgreSQL 或 Ollama 加入开机自启动。

## 4. 创建专用数据库与扩展

下面的 SQL 只针对本地实验数据库。先连接默认的 `postgres` 数据库：

```sql
CREATE ROLE java_ai_knowledge LOGIN PASSWORD 'replace-with-your-database-password';
CREATE DATABASE java_ai_knowledge OWNER java_ai_knowledge;
```

再连接 `java_ai_knowledge`，创建扩展并查看实际版本：

```sql
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

SELECT extname, extversion
FROM pg_extension
WHERE extname IN ('vector', 'pg_trgm')
ORDER BY extname;
```

把同一密码写入唯一的 `config/application-default.yml`：

```yaml
java-ai:
  knowledge:
    mode: postgres-rag
    embedding:
      mode: provider
    postgres:
      jdbc-url: jdbc:postgresql://localhost:5432/java_ai_knowledge
      username: java_ai_knowledge
      password: replace-with-your-database-password
```

在 IDEA 中打开 `learning-stages/stage-03-enterprise-rag`，Working directory 选该阶段目录，运行 `KnowledgeServiceApplication`。随后打开 `rag-learning-journey.http`，从 `01-health` 开始按顺序执行。

## 5. 中断后继续与从头重跑

请求名称就是检查点。中途失败时修好配置，从失败的请求继续，不要重复上传已经成功的固定文档；重复文档 ID 或旧 revision 返回 `409` 是并发保护，不是数据库故障。

确实要从头重跑时，只能删除专用学习数据库，不能对共享库执行下面的操作：

```sql
DROP DATABASE java_ai_knowledge WITH (FORCE);
CREATE DATABASE java_ai_knowledge OWNER java_ai_knowledge;
```

重新连接新数据库并创建 `vector`、`pg_trgm` 后，再运行应用。Flyway 会重建业务表，`.http` 文件中的稳定 ID 又可以从第一条上传开始执行。

常见失败可以直接按下面定位：

| 现象 | 原因 | 处理 |
|---|---|---|
| `extension "vector" is not available` | pgvector 没装进当前 PostgreSQL 17 | 确认 `pg_config` 和 pgvector 指向同一 PostgreSQL |
| `connection refused` | PostgreSQL 没有手动启动或端口不同 | 启动实例并核对 JDBC URL |
| `embedding dimensions ... do not match expected 1536` | Embedding Provider 忽略了 1536 维请求 | 换支持维度参数的模型，或为新维度建立新的向量 Schema |
| 上传或发布返回 `409` | 固定文档已经存在或 revision 过期 | 从下一检查点继续，或仅重建专用学习库 |
| Worker 返回 `FAILED` | Embedding API、模型名或数据库写入失败 | 查看服务错误码，修复后等待重试或重新领取到期任务 |

## 6. 远程接口没有 Embedding 时使用 Ollama

Ollama 是一个本地 Embedding API，只是替代 Provider，不会进入业务代码。安装 [Ollama for macOS](https://docs.ollama.com/macos) 或 [Ollama for Windows](https://docs.ollama.com/windows)，并在系统登录项中关闭它的自动启动。

需要时手动打开 Ollama，下载模型：

```bash
ollama pull qwen3-embedding:4b
```

只在同一份 `config/application-default.yml` 中把 Embedding 切换为 Ollama；Chat 仍使用上面的统一远程 API：

```yaml
spring:
  ai:
    model:
      chat: openai
      embedding: ollama
    ollama:
      base-url: http://localhost:11434
      embedding:
        model: qwen3-embedding:4b
        truncate: true
```

RAG 完成后退出 Ollama，并用前面的停止命令关闭 PostgreSQL。两者都不需要常驻或开机启动。
