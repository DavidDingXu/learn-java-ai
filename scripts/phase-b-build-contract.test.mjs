import assert from "node:assert/strict";
import { existsSync, readFileSync, statSync } from "node:fs";
import path from "node:path";
import test from "node:test";
import { fileURLToPath } from "node:url";

const projectRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");

function read(relativePath) {
  const absolutePath = path.join(projectRoot, relativePath);
  assert.equal(existsSync(absolutePath), true, `${relativePath} must exist`);
  return readFileSync(absolutePath, "utf8");
}

test("root build locks the Phase B dependency and plugin versions", () => {
  const pom = read("pom.xml");

  assert.match(pom, /<resilience4j\.version>2\.4\.0<\/resilience4j\.version>/);
  assert.match(pom, /<swagger-parser\.version>2\.1\.45<\/swagger-parser\.version>/);
  assert.match(pom, /<json-schema-validator\.version>3\.0\.6<\/json-schema-validator\.version>/);
  assert.match(pom, /<maven-shade\.version>3\.6\.2<\/maven-shade\.version>/);
  assert.match(pom, /<mockwebserver\.version>4\.12\.0<\/mockwebserver\.version>/);
  assert.match(pom, /<bannedDependencies>/);
  assert.match(pom, /<exclude>com\.alibaba\.cloud\.ai:\*<\/exclude>/);
  assert.match(pom, /<exclude>dev\.langchain4j:\*<\/exclude>/);
  assert.match(pom, /<exclude>io\.agentscope:\*<\/exclude>/);
});

test("knowledge service owns WebFlux, Spring AI and model resilience", () => {
  const pom = read("services/knowledge-service/pom.xml");

  for (const artifact of [
    "spring-boot-starter-webflux",
    "spring-boot-starter-validation",
    "spring-boot-starter-security",
    "spring-security-oauth2-resource-server",
    "spring-security-oauth2-jose",
    "spring-ai-starter-model-openai",
    "spring-boot-micrometer-tracing-opentelemetry",
    "micrometer-tracing-bridge-otel",
    "micrometer-registry-prometheus",
    "resilience4j-spring-boot4",
    "resilience4j-reactor",
    "caffeine",
    "spring-security-test",
    "mockwebserver",
    "reactor-test",
  ]) {
    assert.match(pom, new RegExp(`<artifactId>${artifact}<\\/artifactId>`));
  }

  assert.doesNotMatch(pom, /<artifactId>spring-boot-starter-web<\/artifactId>/);
});

test("customer BFF owns WebFlux identity infrastructure but no Spring AI", () => {
  const pom = read("apps/customer-bff/pom.xml");

  for (const artifact of [
    "spring-boot-starter-webflux",
    "spring-boot-starter-webclient",
    "spring-boot-starter-validation",
    "spring-boot-starter-security",
    "spring-security-oauth2-resource-server",
    "spring-security-oauth2-jose",
    "spring-security-oauth2-client",
    "spring-security-test",
    "mockwebserver",
  ]) {
    assert.match(pom, new RegExp(`<artifactId>${artifact}<\\/artifactId>`));
  }

  assert.doesNotMatch(pom, /org\.springframework\.ai|<artifactId>spring-ai-/);
});

test("eval runner owns contract tooling and remains independent of services", () => {
  const pom = read("quality/eval-runner/pom.xml");

  for (const artifact of [
    "jackson-databind",
    "swagger-parser-v3",
    "json-schema-validator",
    "maven-shade-plugin",
  ]) {
    assert.match(pom, new RegExp(`<artifactId>${artifact}<\\/artifactId>`));
  }

  assert.doesNotMatch(pom, /<artifactId>swagger-parser<\/artifactId>/);
  assert.match(pom, /ServicesResourceTransformer/);
  assert.doesNotMatch(pom, /knowledge-service|ticket-agent-service|customer-bff|spring-ai-/);
});

test("default model mode is disabled and live mode requires explicit variables", () => {
  const defaults = read("services/knowledge-service/src/main/resources/application.yml");
  const live = read("services/knowledge-service/src/main/resources/application-live-model.yml");

  assert.match(defaults, /speech:\s*none/);
  assert.match(defaults, /transcription:\s*none/);
  assert.match(defaults, /chat:\s*none/);
  assert.match(defaults, /embedding:\s*none/);
  assert.match(defaults, /image:\s*none/);
  assert.match(defaults, /moderation:\s*none/);
  assert.match(defaults, /execution-mode:\s*LOCAL_DISABLED/);
  assert.match(defaults, /external-integrations-enabled:\s*false/);

  assert.match(live, /speech:\s*none/);
  assert.match(live, /transcription:\s*none/);
  assert.match(live, /chat:\s*openai/);
  assert.match(live, /embedding:\s*none/);
  assert.match(live, /image:\s*none/);
  assert.match(live, /moderation:\s*none/);
  assert.match(live, /\$\{JAVA_AI_CHAT_API_KEY}/);
  assert.match(live, /\$\{JAVA_AI_CHAT_BASE_URL}/);
  assert.match(live, /\$\{JAVA_AI_CHAT_MODEL}/);
  assert.match(live, /execution-mode:\s*LIVE_MODEL/);
  assert.match(live, /external-integrations-enabled:\s*true/);
});

test("Phase B verification is cross-platform and Docker-free", {
  skip: process.platform === "win32",
}, () => {
  const shellPath = path.join(projectRoot, "scripts/verify-phase-b.sh");
  const shell = read("scripts/verify-phase-b.sh");
  const powershell = read("scripts/verify-phase-b.ps1");

  assert.notEqual(statSync(shellPath).mode & 0o111, 0);
  for (const content of [shell, powershell]) {
    assert.match(content, /verify-unit/);
    assert.doesNotMatch(content, /node(?:\.exe)?\s+--test/);
    assert.doesNotMatch(content, /docker|testcontainers/i);
  }
});

test("CI proves real JDK 21 and separate JDK 8 builds", () => {
  const verifyWorkflow = read(".github/workflows/phase-b-verify.yml");

  assert.match(verifyWorkflow, /node-version:\s*["']?24/);
  assert.match(verifyWorkflow, /java-version:\s*["']?21/);
  assert.match(verifyWorkflow, /java-version:\s*["']?8/);
  assert.match(verifyWorkflow, /JAVA_AI_MAIN_JAVA_HOME=\$JAVA_HOME/);
  assert.match(verifyWorkflow, /JAVA_AI_JDK8_HOME=\$JAVA_HOME/);
  assert.match(verifyWorkflow, /verify-phase-b\.sh/);
});
