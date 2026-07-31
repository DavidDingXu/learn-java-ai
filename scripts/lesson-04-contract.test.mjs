import assert from "node:assert/strict";
import { existsSync, readFileSync, readdirSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

function filesUnder(relativeDirectory) {
  const absoluteDirectory = path.join(projectRoot, relativeDirectory);
  return readdirSync(absoluteDirectory).flatMap((entry) => {
    const relativePath = path.join(relativeDirectory, entry);
    return statSync(path.join(projectRoot, relativePath)).isDirectory()
      ? filesUnder(relativePath)
      : [relativePath];
  });
}

test("Spring AI remains inside the knowledge infrastructure adapter", () => {
  const javaFiles = filesUnder("services/knowledge-service/src/main/java")
    .filter((file) => file.endsWith(".java"));

  for (const javaFile of javaFiles) {
    const source = read(javaFile);
    if (source.includes("org.springframework.ai")) {
      assert.match(javaFile, /(?:answer|retrieval)\/infrastructure\//);
    }
  }

  const adapter = read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/infrastructure/SpringAiKnowledgeAnswerModel.java");
  assert.match(adapter, /Mono\.fromCallable/);
  assert.match(adapter, /Schedulers\.boundedElastic\(\)/);
});

test("lesson 04 ships deterministic policy without exposing verification modes", () => {
  const policy = read("services/knowledge-service/src/main/resources/knowledge/refund-policy-v1.md");
  const metadata = read("services/knowledge-service/src/main/resources/knowledge/refund-policy-v1.properties");
  const runtime = read("services/knowledge-service/src/main/resources/application.yml");
  const demoConfig = read("config/application.example.yml");
  const openApi = read("contracts/openapi/knowledge-service-v1.yaml");
  const response = read("services/knowledge-service/src/main/java/com/xiaoding/javaai/knowledge/answer/web/KnowledgeAnswerResponse.java");

  assert.match(policy, /1 到 5 个工作日/);
  assert.match(metadata, /documentId=refund-policy/);
  assert.match(metadata, /version=v1/);
  assert.match(metadata, /sectionId=arrival-time/);
  assert.match(demoConfig, /api-key:\s*replace-with-your-api-key/);
  assert.doesNotMatch(runtime, /JAVA_AI_CHAT_(?:API_KEY|BASE_URL|MODEL)/);
  assert.doesNotMatch(runtime, /execution-mode/);
  assert.doesNotMatch(openApi, /executionMode|LOCAL_DISABLED|PROVIDER_PROTOCOL_FIXTURE/);
  assert.doesNotMatch(response, /ExecutionMode|executionMode/);
});

test("live model smoke is cross-platform, YAML-configured and report-producing", () => {
  const shell = read("scripts/run-live-model-smoke.sh");
  const powershell = read("scripts/run-live-model-smoke.ps1");
  const workflow = read(".github/workflows/live-model-smoke.yml");
  const runbook = read("docs/runbooks/live-model-smoke.md");
  const report = read("docs/reports/lesson-04-live-model-smoke.md");
  const demoConfig = read("config/application.example.yml");

  for (const content of [shell, powershell]) {
    assert.match(content, /config[\\/]application\.yml/);
    assert.match(content, /config[\\/]application\.example\.yml/);
    assert.match(content, /spring\.config\.additional-location/);
    assert.doesNotMatch(content, /JAVA_AI_CHAT_(?:API_KEY|BASE_URL|MODEL)/);
  }
  assert.match(demoConfig, /api-key:\s*replace-with-your-api-key/);
  assert.match(demoConfig, /base-url:\s*https:\/\/api\.openai\.com\/v1/);
  assert.match(demoConfig, /model:\s*gpt-4\.1-mini/);
  assert.match(runbook, /config[\\/]application\.yml/);
  assert.match(runbook, /生产环境/);
  assert.match(workflow, /JAVA_AI_CHAT_API_KEY/);

  assert.match(shell, /LiveModelSmokeIT/);
  assert.match(powershell, /LiveModelSmokeIT/);
  assert.match(workflow, /workflow_dispatch/);
  assert.match(workflow, /actions\/upload-artifact/);
  assert.match(report, /Status: (?:NOT_RUN|LIVE_MODEL)/);
  if (/Status: LIVE_MODEL/.test(report)) {
    assert.match(report, /Execution mode: `LIVE_MODEL`/);
    assert.match(report, /Trace ID: `(?:untraced|[0-9a-f]{32})`/);
    assert.doesNotMatch(report, /sk-[A-Za-z0-9]{12,}/);
  } else {
    assert.match(report, /No model credentials were available|未提供模型凭据/i);
  }
  assert.doesNotMatch(report, /PROVIDER_PROTOCOL_FIXTURE.*LIVE_MODEL|LIVE_MODEL.*PROVIDER_PROTOCOL_FIXTURE/s);
});

test("local model credentials stay in an ignored YAML copied from the tracked example", () => {
  const ignore = read(".gitignore");
  const example = read("config/application.example.yml");
  const shell = read("scripts/run-live-model-smoke.sh");
  const powershell = read("scripts/run-live-model-smoke.ps1");

  assert.match(ignore, /^config\/application\.yml$/m);
  assert.doesNotMatch(ignore, /^config\/application\.example\.yml$/m);
  assert.match(example, /api-key:\s*replace-with-your-api-key/);
  assert.match(shell, /cp\s+"\$EXAMPLE_CONFIG_FILE"\s+"\$CONFIG_FILE"/);
  assert.match(powershell, /Copy-Item\s+-Path\s+\$ExampleConfigFile\s+-Destination\s+\$ConfigFile/);
});

test("live model tests validate the report path before calling the provider", () => {
  const cases = [
    [
      "services/knowledge-service/src/test/java/com/xiaoding/javaai/knowledge/answer/LiveModelSmokeIT.java",
      "java-ai.smoke.report-path",
      "answerKnowledgeQuestion.answer",
    ],
    [
      "services/ticket-agent-service/src/test/java/com/xiaoding/javaai/ticket/agent/infrastructure/TicketAgentLiveModelSmokeIT.java",
      "java-ai.agent-smoke.report-path",
      "planner.plan",
    ],
  ];

  for (const [relativePath, property, providerCall] of cases) {
    const source = read(relativePath);
    const validation = source.indexOf(`requiredSystemProperty("${property}")`);
    assert.notEqual(validation, -1, `${relativePath} must require ${property}`);
    assert.ok(validation < source.indexOf(providerCall), `${relativePath} must validate before ${providerCall}`);
  }
});

test("live model smoke validates the YAML placeholder without reading environment variables", () => {
  for (const relativePath of [
    "services/knowledge-service/src/test/java/com/xiaoding/javaai/knowledge/answer/LiveModelSmokeIT.java",
    "services/ticket-agent-service/src/test/java/com/xiaoding/javaai/ticket/agent/infrastructure/TicketAgentLiveModelSmokeIT.java",
  ]) {
    const source = read(relativePath);
    assert.match(source, /spring\.ai\.openai\.api-key/);
    assert.match(source, /replace-with-your-api-key/);
    assert.match(source, /config\/application\.yml/);
    assert.doesNotMatch(source, /System\.getenv|requiredEnvironment/);
  }
});

test("lesson 04 milestone status follows the real model evidence", () => {
  const milestone = read("docs/reports/milestone-04.md");
  const report = read("docs/reports/lesson-04-live-model-smoke.md");

  if (/VERIFIED_LIVE_MODEL/.test(milestone)) {
    assert.match(report, /Status: LIVE_MODEL/);
    assert.match(milestone, /VERIFIED_LIVE_MODEL/);
    assert.match(milestone, /lesson-04-live-model-smoke\.md/);
    assert.doesNotMatch(milestone, /Tag Rule|必须指向|must point/i);
  } else {
    assert.match(milestone, /PENDING_LIVE_MODEL/);
    assert.match(milestone, /provider protocol fixture/i);
    assert.match(milestone, /must not create|不得创建/i);
  }
});
