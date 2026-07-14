import assert from "node:assert/strict";
import { existsSync, readFileSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

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
  assert.match(ownership, /must not own|不得拥有|不能拥有/i);
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
  assert.match(decision, /business language|业务语言/i);
  assert.match(decision, /does not create a universal model gateway|不建设万能模型网关/i);
  assert.match(decision, /Replacement Conditions|替换条件/i);
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
    "Spring team fit",
    "Chat/Output/RAG/Tool/MCP",
    "Agent runtime",
    "Security/Observation integration",
    "Provider reach",
    "Dependency risk",
    "Exit cost",
    "Decision",
  ]) {
    assert.match(matrix, new RegExp(axis.replaceAll("/", "\\/"), "i"));
  }

  assert.match(matrix, /same business contract|同一业务合同/i);
  assert.match(matrix, /same dataset|同一数据集/i);
});

test("lesson 02 and 03 reports bind decisions to reviewed evidence", () => {
  const reports = [
    read("docs/reports/lesson-02-architecture-review.md"),
    read("docs/reports/lesson-03-framework-decision.md"),
  ];

  for (const report of reports) {
    assert.match(report, /Reviewed files|复核文件/i);
    assert.match(report, /Commit|提交基线/i);
    assert.match(report, /3be7f19/);
    assert.match(report, /Accepted boundaries|接受的边界/i);
    assert.match(report, /Rejected alternatives|拒绝的方案/i);
    assert.match(report, /Replacement conditions|替换条件/i);
    assert.doesNotMatch(report, /runtime experiment passed|运行实验通过|生产可用/);
  }
});
