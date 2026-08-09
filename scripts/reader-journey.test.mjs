import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const readerPaths = [
  "README.md",
  "services/knowledge-service/README.md",
  "services/ticket-agent-service/README.md",
  "apps/customer-bff/README.md",
  "apps/customer-web/README.md",
  "integrations/jdk8-client/README.md",
  "labs/README.md",
  "labs/agentscope-lab/README.md",
  "labs/langchain4j-lab/README.md",
  "labs/protocol-interop-lab/README.md",
  "labs/spring-ai-alibaba-lab/README.md",
  "docs/README.md",
  "docs/runbooks/local-toolchain.md",
  "docs/runbooks/runtime-configuration.md",
  "docs/runbooks/knowledge-ingestion.md",
  "docs/runbooks/live-model-smoke.md",
  "docs/runbooks/model-interaction-eval.md",
  "docs/runbooks/security-regression.md",
  "learning-stages/README.md",
];

function read(relativePath) {
  return readFileSync(path.join(projectRoot, relativePath), "utf8");
}

function findFile(directory, expectedName) {
  for (const entry of readdirSync(directory)) {
    const absolutePath = path.join(directory, entry);
    if (statSync(absolutePath).isDirectory()) {
      const nested = findFile(absolutePath, expectedName);
      if (nested) return nested;
    } else if (entry === expectedName) {
      return absolutePath;
    }
  }
  return null;
}

test("reader journey starts applications instead of build or test commands", () => {
  const forbiddenCommand = /(?:\.\/mvnw|\.\\mvnw\.cmd|\bmvn\s|npm\s+test|npm\s+run\s+(?:typecheck|build)|运行测试|执行测试)/i;
  const violations = readerPaths.filter((relativePath) => forbiddenCommand.test(read(relativePath)));

  assert.deepEqual(violations, []);

  const entry = read("README.md");
  assert.match(entry, /config\/application\.yml/);
  assert.match(entry, /spring\.ai\.openai\.api-key/);
  assert.match(entry, /KnowledgeServiceApplication/);
  assert.match(entry, /POST http:\/\/localhost:8081\/api\/v1\/knowledge\/answers/);
});

test("reader journey keeps local model settings in YAML instead of shell variables", () => {
  const shellEnvironmentSetup = /\b(?:AI_API_KEY|AI_BASE_URL|OPENAI_API_KEY)\b|\bexport\s+[A-Z_][A-Z0-9_]*=|\$env:[A-Z_][A-Z0-9_]*/i;
  const violations = readerPaths.filter((relativePath) => shellEnvironmentSetup.test(read(relativePath)));

  assert.deepEqual(violations, []);
  const sharedConfig = read("config/application.yml");
  assert.match(sharedConfig, /api-key:\s*replace-with-your-api-key/);
  assert.match(sharedConfig, /embedding:\s*none/);
  assert.match(sharedConfig, /mode:\s*ollama/);
  assert.match(sharedConfig, /model:\s*qwen3-embedding:4b/);
});

test("all runnable backend modules name their direct IDE entrypoint", () => {
  const expectedEntrypoints = new Map([
    ["services/knowledge-service/README.md", "KnowledgeServiceApplication"],
    ["services/ticket-agent-service/README.md", "TicketAgentServiceApplication"],
    ["apps/customer-bff/README.md", "CustomerBffApplication"],
  ]);

  for (const [relativePath, entrypoint] of expectedEntrypoints) {
    assert.match(read(relativePath), new RegExp(entrypoint), relativePath);
  }
});

test("seven learning stages are independent runnable modules", () => {
  const stages = [
    ["stage-01-system-boundaries", "SystemBoundariesStageApplication"],
    ["stage-02-model-engineering", "ModelEngineeringStageApplication"],
    ["stage-03-enterprise-rag", "EnterpriseRagStageApplication"],
    ["stage-04-customer-consultation", "CustomerConsultationStageApplication"],
    ["stage-05-controlled-agent", "ControlledAgentStageApplication"],
    ["stage-06-production-readiness", "ProductionReadinessStageApplication"],
    ["stage-07-framework-boundaries", "FrameworkBoundariesStageApplication"],
  ];
  const stagePom = read("learning-stages/pom.xml");
  const stageReadme = read("learning-stages/README.md");

  for (const [moduleName, application] of stages) {
    assert.match(stagePom, new RegExp(`<module>${moduleName}</module>`));
    assert.match(stageReadme, new RegExp(application));
    const javaFiles = readFileSync(
      path.join(projectRoot, "learning-stages", moduleName, "pom.xml"),
      "utf8",
    );
    assert.match(javaFiles, /stage-support/);
    const applicationFile = findFile(
      path.join(projectRoot, "learning-stages", moduleName, "src", "main", "java"),
      `${application}.java`,
    );
    assert.equal(existsSync(applicationFile ?? ""), true, `${application}.java must exist`);
    assert.match(readFileSync(applicationFile, "utf8"), /public static void main\(String\[\] args\)/);
  }
  assert.match(read("learning-stages/config/application.yml"), /knowledge-base-url:/);
});
