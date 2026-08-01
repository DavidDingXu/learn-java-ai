import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

function markdownFiles(relativeDirectory) {
  const absoluteDirectory = path.join(projectRoot, relativeDirectory);
  return readdirSync(absoluteDirectory, { withFileTypes: true }).flatMap((entry) => {
    if ([".git", "node_modules", "target"].includes(entry.name)) return [];
    const relativePath = path.join(relativeDirectory, entry.name);
    if (entry.isDirectory()) return markdownFiles(relativePath);
    return entry.isFile() && entry.name.endsWith(".md") ? [relativePath] : [];
  });
}

test("public project documentation avoids opaque authoring language", () => {
  const publicDocuments = markdownFiles(".");
  const opaqueWording = /合同|收口|闭环|沉淀|教学(?:样例|数据|回归)|\bthe\s+column\b|\bcolumn\s+completion\b|\bbusiness\s+contracts?\b|\bteaching\s+(?:cases|data|regressions?)\b|\bPhase\s+[A-Z]\b|\bReset\s+Delivery\b/i;
  const violations = publicDocuments.flatMap((relativePath) =>
    read(relativePath)
      .split(/\r?\n/)
      .flatMap((line, index) =>
        opaqueWording.test(line) ? [`${relativePath}:${index + 1}`] : [],
      ),
  );

  assert.deepEqual(violations, []);
});

test("architecture documents match the executable module boundaries", () => {
  const rootPom = read("pom.xml");
  const context = read("docs/architecture/system-context.md");
  const ownership = read("docs/architecture/service-ownership.md");

  for (const moduleName of [
    "services/knowledge-service",
    "services/ticket-agent-service",
    "apps/customer-bff",
    "quality/eval-runner",
  ]) {
    assert.match(rootPom, new RegExp(`<module>${moduleName}<\\/module>`));
  }

  for (const component of [
    "Customer BFF",
    "Knowledge Service",
    "Ticket Agent Service",
    "JDK8",
    "Eval Runner",
  ]) {
    assert.match(`${context}\n${ownership}`, new RegExp(component, "i"));
  }

  assert.match(ownership, /owns|拥有/i);
  assert.match(ownership, /must not own|不得拥有|不能拥有|不应拥有/i);
});

test("service collaboration is contract-first and forbids shared domain state", () => {
  const documents = [
    read("docs/architecture/system-context.md"),
    read("docs/architecture/customer-consultation-flow.md"),
    read("docs/architecture/service-ownership.md"),
  ].join("\n");

  assert.match(documents, /HTTP\/OpenAPI|HTTP.*OpenAPI/i);
  assert.match(documents, /不共享领域 JAR|no shared domain JAR/i);
  assert.match(documents, /不跨服务.*数据库|不得.*另一个服务.*数据库|do not.*another service.*database/i);
  assert.match(documents, /不使用共享数据库作为集成方案|shared database.*prohibited/i);
  assert.match(documents, /delegated identity|委托身份/i);
  assert.match(documents, /人工确认|human confirmation/i);
});

test("Spring AI stays inside business-specific service adapters, not a universal gateway", () => {
  const knowledgePom = read("services/knowledge-service/pom.xml");
  const bffPom = read("apps/customer-bff/pom.xml");
  const ticketPom = read("services/ticket-agent-service/pom.xml");
  const decision = read("docs/adr/0001-spring-ai-mainline.md");

  assert.match(knowledgePom, /spring-ai-starter-model-openai/);
  assert.match(ticketPom, /spring-ai-starter-model-openai/);
  assert.doesNotMatch(bffPom, /org\.springframework\.ai|spring-ai-/);
  assert.match(decision, /infrastructure adapters|基础设施适配器/i);
  assert.match(decision, /business language|业务语言|业务语义/i);
  assert.match(decision, /does not create a universal model gateway|不建设(?:万能|通用)模型网关/i);
  assert.match(decision, /Replacement Conditions|替换条件|重新评审条件/i);
});

test("service security never makes configuration or metrics endpoints anonymous", () => {
  const securityConfigurations = [
    read("apps/customer-bff/src/main/java/com/xiaoding/javaai/customer/SecurityConfiguration.java"),
    read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/SecurityConfiguration.java"),
    read("services/ticket-agent-service/src/main/java/com/xiaoding/javaai/ticket/SecurityConfiguration.java"),
  ].join("\n");

  assert.doesNotMatch(securityConfigurations, /pathMatchers\([^)]*\/actuator\/env/s);
  assert.doesNotMatch(securityConfigurations, /requestMatchers\([^)]*\/actuator\/env/s);
  assert.doesNotMatch(securityConfigurations, /pathMatchers\([^)]*\/actuator\/prometheus/s);
  assert.doesNotMatch(securityConfigurations, /requestMatchers\([^)]*\/actuator\/prometheus/s);
});

test("framework matrix compares production candidates on the same decision axes", () => {
  const matrix = read("docs/decisions/framework-selection-matrix.md");

  for (const candidate of [
    "Spring AI",
    "Spring AI Alibaba",
    "LangChain4j",
    "AgentScope",
    "Provider SDK",
    "Existing company AI platform",
  ]) {
    assert.match(matrix, new RegExp(candidate, "i"));
  }

  for (const axis of [
    "Spring 团队适配度",
    "Chat/Output/RAG/Tool/MCP",
    "Agent 运行时",
    "安全与可观测集成",
    "Provider 覆盖",
    "依赖风险",
    "退出成本",
    "决策",
  ]) {
    assert.match(matrix, new RegExp(axis.replaceAll("/", "\\/"), "i"));
  }

  assert.match(matrix, /same (?:business )?interfaces|相同(?:的)?业务接口/i);
  assert.match(matrix, /same dataset|相同[^。；\n]{0,24}数据集/i);
});

test("architecture and framework reviews keep decision evidence explicit", () => {
  const reports = [
    read("docs/reports/lesson-02-architecture-review.md"),
    read("docs/reports/lesson-03-framework-decision.md"),
  ];

  for (const report of reports) {
    assert.match(report, /Reviewed files|复核文件|复核依据/i);
    assert.match(report, /Accepted boundaries|接受的边界|采用的边界/i);
    assert.match(report, /Rejected alternatives|拒绝的方案|未采用的方案/i);
    assert.match(report, /Replacement conditions|替换条件|重新评估条件|重新选型的条件/i);
    assert.doesNotMatch(report, /runtime experiment passed|运行实验通过|生产可用/);
    assert.doesNotMatch(report, /Implementation commit|Input baseline commit|提交基线/);
  }
});
