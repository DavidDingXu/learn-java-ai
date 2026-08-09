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
  assert.match(sharedConfig, /embedding:\s*openai/);
  assert.match(sharedConfig, /ollama:[\s\S]*?base-url:\s*http:\/\/localhost:11434/);
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

test("seven learning stages contain real capability slices", () => {
  const stages = new Map([
    ["stage-01-system-boundaries", [
      "services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/KnowledgeServiceApplication.java",
      "services/ticket-agent-service/src/main/java/com/xiaoding/javaai/ticket/TicketAgentServiceApplication.java",
      "apps/customer-bff/src/main/java/com/xiaoding/javaai/customer/CustomerBffApplication.java",
    ]],
    ["stage-02-model-engineering", [
      "services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/infrastructure/SpringAiKnowledgeAnswerModel.java",
      "services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/application/StreamingKnowledgeAnswerService.java",
      "quality/eval-runner/src/main/java/com/xiaoding/javaai/eval/model/ModelInteractionEvaluator.java",
    ]],
    ["stage-03-enterprise-rag", [
      "services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/document/application/DocumentUploadService.java",
      "services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/retrieval/application/HybridKnowledgeRetrievalService.java",
      "services/knowledge-service/src/main/resources/db/migration/V1__knowledge_platform.sql",
    ]],
    ["stage-04-customer-consultation", [
      "apps/customer-bff/src/main/java/com/xiaoding/javaai/customer/consultation/application/CustomerConsultationService.java",
      "apps/customer-bff/src/main/java/com/xiaoding/javaai/customer/consultation/domain/ConsultationSession.java",
    ]],
    ["stage-05-controlled-agent", [
      "services/ticket-agent-service/src/main/java/com/xiaoding/javaai/ticket/agent/application/TicketAgentOrchestrator.java",
      "services/ticket-agent-service/src/main/java/com/xiaoding/javaai/ticket/agent/application/ToolConfirmationService.java",
      "integrations/jdk8-client/src/main/java/com/xiaoding/javaai/legacy/ticket/TicketAgentClient.java",
    ]],
    ["stage-06-production-readiness", [
      "quality/eval-runner/src/main/java/com/xiaoding/javaai/eval/EvalRunner.java",
      "datasets/security/agent-security-v1.jsonl",
    ]],
    ["stage-07-framework-boundaries", [
      "labs/spring-ai-alibaba-lab/src/main/java/com/xiaoding/javaai/labs/alibaba/SpringAiAlibabaLabApplication.java",
      "labs/spring-ai-alibaba-lab/src/main/java/com/xiaoding/javaai/labs/alibaba/DashScopeProviderAdapter.java",
      "labs/langchain4j-lab/src/main/java/com/xiaoding/javaai/labs/langchain4j/LangChain4jLabApplication.java",
      "labs/langchain4j-lab/src/main/java/com/xiaoding/javaai/labs/langchain4j/LangChain4jPolicyAnswerAdapter.java",
      "labs/agentscope-lab/src/main/java/com/xiaoding/javaai/labs/agentscope/AgentScopeLabApplication.java",
      "labs/agentscope-lab/src/main/java/com/xiaoding/javaai/labs/agentscope/AgentScopeTicketRuntime.java",
      "labs/protocol-interop-lab/src/main/java/com/xiaoding/javaai/labs/protocol/McpLabApplication.java",
      "labs/protocol-interop-lab/src/main/java/com/xiaoding/javaai/labs/protocol/A2aLabApplication.java",
    ]],
  ]);

  assert.equal(existsSync(path.join(projectRoot, "learning-stages", "stage-support")), false);
  for (const [stage, requiredFiles] of stages) {
    assert.equal(existsSync(path.join(projectRoot, "learning-stages", stage, "pom.xml")), true);
    assert.equal(existsSync(path.join(projectRoot, "learning-stages", stage, "README.md")), true);
    for (const requiredFile of requiredFiles) {
      assert.equal(
        existsSync(path.join(projectRoot, "learning-stages", stage, requiredFile)),
        true,
        `${stage} must contain real source: ${requiredFile}`,
      );
    }
  }

  assert.equal(
    existsSync(path.join(projectRoot, "learning-stages/stage-04-customer-consultation/services/knowledge-service")),
    false,
  );
  assert.equal(
    existsSync(path.join(projectRoot, "learning-stages/stage-05-controlled-agent/apps/customer-bff")),
    false,
  );
  assert.equal(
    existsSync(path.join(projectRoot, "learning-stages/stage-07-framework-boundaries/services")),
    false,
  );
});

test("learning snapshots do not reintroduce teaching-only runtime glue", () => {
  const forbiddenNames = ["StageHttp.java", "StageConfig.java", "StageOutput.java"];
  for (const forbiddenName of forbiddenNames) {
    assert.equal(findFile(path.join(projectRoot, "learning-stages"), forbiddenName), null);
  }

  for (const entry of readdirSync(path.join(projectRoot, "learning-stages"))) {
    if (!entry.startsWith("stage-")) continue;
    const yamlRoot = path.join(projectRoot, "learning-stages", entry);
    const stack = [yamlRoot];
    while (stack.length > 0) {
      const current = stack.pop();
      for (const child of readdirSync(current, { withFileTypes: true })) {
        if (child.name === "target") continue;
        const absolutePath = path.join(current, child.name);
        if (child.isDirectory()) stack.push(absolutePath);
        if (child.isFile() && /^application.*\.ya?ml$/.test(child.name)) {
          assert.doesNotMatch(readFileSync(absolutePath, "utf8"), /\$\{[A-Z][A-Z0-9_]*/);
        }
      }
    }
  }
});
