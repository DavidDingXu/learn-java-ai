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

function tagValue(xml, tagName) {
  const match = xml.match(new RegExp(`<${tagName}>([^<]+)</${tagName}>`));
  return match?.[1].trim();
}

function tagBlock(xml, tagName) {
  const match = xml.match(new RegExp(`<${tagName}>([\\s\\S]*?)</${tagName}>`));
  return match?.[1] ?? "";
}

function moduleNames(xml) {
  const modules = tagBlock(xml, "modules");
  return [...modules.matchAll(/<module>([^<]+)<\/module>/g)].map((match) => match[1].trim());
}

function dependencyManagementCoordinates(xml) {
  const managed = tagBlock(xml, "dependencyManagement");
  return [...managed.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)].map((match) => {
    const dependency = match[1];
    return `${tagValue(dependency, "groupId")}:${tagValue(dependency, "artifactId")}:${tagValue(dependency, "version")}`;
  });
}

function dependencyCoordinates(xml) {
  return [...xml.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)]
    .map((match) => ({
      groupId: tagValue(match[1], "groupId"),
      artifactId: tagValue(match[1], "artifactId"),
    }))
    .filter((coordinate) => coordinate.groupId && coordinate.artifactId);
}

function isRestrictedInfrastructureDependency({ groupId, artifactId }) {
  const allowedFrameworkBoms = new Map([
    ["org.springframework.ai", "spring-ai-bom"],
    ["com.alibaba.cloud.ai", "spring-ai-alibaba-bom"],
    ["dev.langchain4j", "langchain4j-bom"],
    ["io.agentscope", "agentscope-bom"],
  ]);
  const forbiddenGroups = new Set([
    "org.testcontainers",
    "org.postgresql",
    "com.pgvector",
    "io.minio",
    "org.apache.kafka",
    "org.springframework.kafka",
  ]);
  const exactArtifacts = new Set([
    "postgresql",
    "pgvector",
    "minio",
    "spring-kafka",
    "spring-boot-starter-data-redis",
    "spring-boot-starter-data-redis-reactive",
    "testcontainers",
  ]);
  const prefixes = [
    "spring-ai-starter-model-",
    "spring-ai-alibaba-starter-",
  ];

  const allowedBom = allowedFrameworkBoms.get(groupId);
  if (allowedBom) {
    return artifactId !== allowedBom;
  }

  return forbiddenGroups.has(groupId)
    || exactArtifacts.has(artifactId)
    || prefixes.some((prefix) => artifactId.startsWith(prefix));
}

function assertParent(relativePom, expectedArtifactId, expectedRelativePath) {
  const pom = read(relativePom);
  const parent = tagBlock(pom, "parent");
  assert.equal(tagValue(parent, "artifactId"), expectedArtifactId, `${relativePom} parent artifactId`);
  assert.equal(tagValue(parent, "relativePath"), expectedRelativePath, `${relativePom} parent relativePath`);
}

function findPomFiles(relativeDirectory) {
  const root = path.join(projectRoot, relativeDirectory);
  if (!existsSync(root)) {
    return [];
  }

  const results = [];
  for (const entry of readdirSync(root)) {
    const entryPath = path.join(root, entry);
    const relativePath = path.relative(projectRoot, entryPath);
    if (statSync(entryPath).isDirectory()) {
      results.push(...findPomFiles(relativePath));
    } else if (entry === "pom.xml") {
      results.push(relativePath);
    }
  }
  return results;
}

test("root reactor contains only the four Java 21 mainline modules", () => {
  const rootPom = read("pom.xml");

  assert.deepEqual(moduleNames(rootPom), [
    "services/knowledge-service",
    "services/ticket-agent-service",
    "apps/customer-bff",
    "quality/eval-runner",
  ]);
  assert.equal(rootPom.includes("labs/"), false);
  assert.equal(rootPom.includes("integrations/jdk8-client"), false);
  assert.equal(rootPom.includes("apps/customer-web"), false);
  assert.equal(rootPom.includes("<java.version>21</java.version>"), true);
  assert.equal(rootPom.includes("<maven.compiler.release>21</maven.compiler.release>"), true);
  assert.equal(rootPom.includes("<spring-boot.version>4.1.0</spring-boot.version>"), true);
  assert.equal(rootPom.includes("<spring-ai.version>2.0.0</spring-ai.version>"), true);
});

test("obsolete demo and project directories are absent", () => {
  const obsoleteDirectories = readdirSync(projectRoot, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .filter((name) => /^ai-.*-demo$/.test(name)
      || name === "ai-common"
      || name === "project-enterprise-rag"
      || name === "project-helpdesk-agent");

  assert.deepEqual(obsoleteDirectories, []);
});

test("mainline child modules inherit only the root parent", () => {
  assertParent("services/knowledge-service/pom.xml", "java-ai-engineering-practice", "../../pom.xml");
  assertParent("services/ticket-agent-service/pom.xml", "java-ai-engineering-practice", "../../pom.xml");
  assertParent("apps/customer-bff/pom.xml", "java-ai-engineering-practice", "../../pom.xml");
  assertParent("quality/eval-runner/pom.xml", "java-ai-engineering-practice", "../../pom.xml");
});

test("framework and protocol labs use an isolated reactor with explicit dependency ownership", () => {
  const labsPom = read("labs/pom.xml");
  assert.equal(tagBlock(labsPom, "parent"), "");
  assert.deepEqual(moduleNames(labsPom), [
    "spring-ai-alibaba-lab",
    "langchain4j-lab",
    "agentscope-lab",
    "protocol-interop-lab",
  ]);
  assert.deepEqual(dependencyManagementCoordinates(labsPom), []);

  const labs = [
    ["spring-ai-alibaba-lab", "com.alibaba.cloud.ai:spring-ai-alibaba-bom:${spring-ai-alibaba.version}"],
    ["langchain4j-lab", "dev.langchain4j:langchain4j-bom:${langchain4j.version}"],
    ["agentscope-lab", "io.agentscope:agentscope-bom:${agentscope.version}"],
  ];
  for (const [moduleName, expectedBom] of labs) {
    const relativePom = `labs/${moduleName}/pom.xml`;
    assertParent(relativePom, "java-ai-framework-labs", "../pom.xml");
    assert.deepEqual(dependencyManagementCoordinates(read(relativePom)), [expectedBom]);
  }

  assertParent("labs/protocol-interop-lab/pom.xml", "java-ai-framework-labs", "../pom.xml");
  assert.deepEqual(
    dependencyManagementCoordinates(read("labs/protocol-interop-lab/pom.xml")),
    [],
  );
});

test("Java 8 client is an independent Maven build", () => {
  const clientPom = read("integrations/jdk8-client/pom.xml");

  assert.equal(tagBlock(clientPom, "parent"), "");
  assert.equal(clientPom.includes("<maven.compiler.release>8</maven.compiler.release>"), true);
  assert.equal(clientPom.includes("<junit.version>5.11.4</junit.version>"), true);
  assert.equal(clientPom.includes("<maven.compiler.version>3.15.0</maven.compiler.version>"), true);
  assert.equal(clientPom.includes("<maven.surefire.version>3.5.6</maven.surefire.version>"), true);
});

test("top-level products and contract directories provide local documentation", () => {
  for (const readme of [
    "apps/customer-web/README.md",
    "apps/customer-bff/README.md",
    "contracts/README.md",
    "datasets/README.md",
    "deploy/README.md",
    "services/knowledge-service/README.md",
    "services/ticket-agent-service/README.md",
    "quality/eval-runner/README.md",
    "integrations/jdk8-client/README.md",
  ]) {
    read(readme);
  }

  assert.deepEqual(findPomFiles("apps/customer-web"), []);
  assert.deepEqual(findPomFiles("contracts"), []);
});

test("Customer Web is an independently verifiable Node 24 product", () => {
  const packageJson = JSON.parse(read("apps/customer-web/package.json"));
  read("apps/customer-web/package-lock.json");

  assert.equal(packageJson.private, true);
  assert.match(packageJson.engines?.node ?? "", /24/);
  for (const scriptName of ["typecheck", "test", "build"]) {
    assert.equal(
      typeof packageJson.scripts?.[scriptName],
      "string",
      `apps/customer-web/package.json must define scripts.${scriptName}`,
    );
    assert.notEqual(packageJson.scripts[scriptName].trim(), "");
  }
});

test("mainline POMs keep infrastructure and model provider ownership explicit", () => {
  const allowedLabDependencies = new Map([
    ["labs/spring-ai-alibaba-lab/pom.xml", new Set([
      "com.alibaba.cloud.ai:spring-ai-alibaba-dashscope",
      "com.alibaba.cloud.ai:spring-ai-alibaba-graph-core",
    ])],
    ["labs/langchain4j-lab/pom.xml", new Set([
      "dev.langchain4j:langchain4j",
    ])],
    ["labs/agentscope-lab/pom.xml", new Set([
      "io.agentscope:agentscope-core",
      "io.agentscope:agentscope-extensions-a2a-client",
    ])],
    ["labs/protocol-interop-lab/pom.xml", new Set([
      "io.modelcontextprotocol.sdk:mcp",
      "org.a2aproject.sdk:a2a-java-sdk-client",
    ])],
  ]);
  const pomPaths = [
    "pom.xml",
    "services/knowledge-service/pom.xml",
    "services/ticket-agent-service/pom.xml",
    "apps/customer-bff/pom.xml",
    "quality/eval-runner/pom.xml",
    "labs/pom.xml",
    "labs/spring-ai-alibaba-lab/pom.xml",
    "labs/langchain4j-lab/pom.xml",
    "labs/agentscope-lab/pom.xml",
    "labs/protocol-interop-lab/pom.xml",
    "integrations/jdk8-client/pom.xml",
  ];
  for (const pomPath of pomPaths) {
    for (const coordinate of dependencyCoordinates(read(pomPath))) {
      const allowedMainlineAiProvider = [
        "services/knowledge-service/pom.xml",
        "services/ticket-agent-service/pom.xml",
      ].includes(pomPath)
        && coordinate.groupId === "org.springframework.ai"
        && coordinate.artifactId === "spring-ai-starter-model-openai";
      const allowedKnowledgeStorage = pomPath === "services/knowledge-service/pom.xml"
        && ((coordinate.groupId === "org.postgresql" && coordinate.artifactId === "postgresql")
          || (coordinate.groupId === "com.pgvector" && coordinate.artifactId === "pgvector"));
      const allowedTicketStorage = pomPath === "services/ticket-agent-service/pom.xml"
        && coordinate.groupId === "org.postgresql"
        && coordinate.artifactId === "postgresql";
      const allowedRootVersionManagement = pomPath === "pom.xml"
        && coordinate.groupId === "com.pgvector"
        && coordinate.artifactId === "pgvector";
      const allowedLabDependency = allowedLabDependencies.get(pomPath)
        ?.has(`${coordinate.groupId}:${coordinate.artifactId}`) ?? false;
      assert.equal(
        allowedMainlineAiProvider
          || allowedKnowledgeStorage
          || allowedTicketStorage
          || allowedRootVersionManagement
          || allowedLabDependency
          || !isRestrictedInfrastructureDependency(coordinate),
        true,
        `forbidden dependency in ${pomPath}: ${coordinate.groupId}:${coordinate.artifactId}`,
      );
    }
  }
});

test("provider starter artifact IDs require an explicit module exception", () => {
  for (const coordinate of [
    { groupId: "org.springframework.ai", artifactId: "spring-ai-starter-model-openai" },
    { groupId: "org.springframework.ai", artifactId: "spring-ai-starter-model-anthropic" },
    { groupId: "com.alibaba.cloud.ai", artifactId: "spring-ai-alibaba-starter-dashscope" },
    { groupId: "dev.langchain4j", artifactId: "langchain4j-open-ai" },
    { groupId: "dev.langchain4j", artifactId: "langchain4j-open-ai-spring-boot-starter" },
    { groupId: "dev.langchain4j", artifactId: "langchain4j-azure-open-ai" },
    { groupId: "io.agentscope", artifactId: "agentscope-extensions-model-openai" },
    { groupId: "io.agentscope", artifactId: "agentscope-openai-spring-boot-starter" },
    { groupId: "org.springframework.boot", artifactId: "spring-boot-starter-data-redis-reactive" },
    { groupId: "org.testcontainers", artifactId: "junit-jupiter" },
  ]) {
    assert.equal(
      isRestrictedInfrastructureDependency(coordinate),
      true,
      `${coordinate.groupId}:${coordinate.artifactId}`,
    );
  }
});

test("Maven Wrapper is pinned to Maven 3.9.14", () => {
  read("mvnw");
  read("mvnw.cmd");
  const wrapperProperties = read(".mvn/wrapper/maven-wrapper.properties");
  assert.match(wrapperProperties, /apache-maven-3\.9\.14-bin\.zip/);
});

test("public README matches the current service and build boundaries", () => {
  const readme = read("README.md");

  for (const requiredPath of [
    "services/knowledge-service",
    "services/ticket-agent-service",
    "apps/customer-bff",
    "quality/eval-runner",
    "integrations/jdk8-client",
    "labs",
  ]) {
    assert.match(readme, new RegExp(requiredPath.replaceAll("/", "\\/")));
  }

  assert.match(readme, /企业场景的 Java AI 应用工程参考实现/);
  assert.match(readme, /受控 Agent/);
  assert.match(readme, /安全回归/);
  assert.match(readme, /Micrometer/);
  assert.match(readme, /跨平台发布门禁/);
  assert.match(readme, /labs.*独立构建/s);
  assert.match(readme, /MCP Java SDK 2\.0\.0/);
  assert.match(readme, /A2A Java SDK 1\.1\.0\.Final/);
  assert.match(readme, /MCP Java SDK 2\.0\.0/);
  assert.match(readme, /A2A Java SDK 1\.1\.0\.Final/);
  assert.match(readme, /Java 8.*客户端/);
  assert.match(readme, /pgvector/);
  assert.match(readme, /verify-unit/);
  assert.match(readme, /HTTP\/OpenAPI/);
  assert.match(readme, /不共享领域 JAR/);
  assert.doesNotMatch(readme, /AiCallGateway|ai-[a-z-]+-demo|project-enterprise-rag|project-helpdesk-agent/);

  const labsReadme = read("labs/README.md");
  for (const lab of [
    "spring-ai-alibaba-lab",
    "langchain4j-lab",
    "agentscope-lab",
    "protocol-interop-lab",
  ]) {
    assert.match(labsReadme, new RegExp(lab));
  }
});

test("public project surface excludes authoring and internal delivery artifacts", () => {
  for (const internalPath of [
    "AGENTS.md",
    "docs/delivery/phase-a-reset.md",
    ".github/workflows/phase-b-verify.yml",
    ".github/workflows/phase-c-pgvector.yml",
    "scripts/phase-b-build-contract.test.mjs",
    "scripts/verify-phase-b.sh",
    "scripts/verify-phase-b.ps1",
  ]) {
    assert.equal(existsSync(path.join(projectRoot, internalPath)), false, `${internalPath} must not be public`);
  }

  const docs = [
    "README.md",
    "docs/architecture/system-context.md",
    "docs/adr/0001-spring-ai-mainline.md",
    "docs/adr/0002-build-boundaries.md",
    "docs/runbooks/local-toolchain.md",
    "docs/version-baseline.md",
    "apps/customer-web/README.md",
    "apps/customer-bff/README.md",
    "services/knowledge-service/README.md",
    "services/ticket-agent-service/README.md",
    "quality/eval-runner/README.md",
    "integrations/jdk8-client/README.md",
  ];

  for (const document of docs) {
    const content = read(document);
    assert.doesNotMatch(content, /付费专栏|公众号|文章排期|备稿|小红书|作者工作流|author workstation|Phase [A-Z]|Reset Delivery/);
  }
});

test("open-source community files and contribution entry points are present", () => {
  for (const publicFile of [
    "LICENSE",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "SUPPORT.md",
    "CODE_OF_CONDUCT.md",
    "docs/README.md",
    "docs/reports/README.md",
    "labs/protocol-interop-lab/README.md",
    ".github/ISSUE_TEMPLATE/bug_report.yml",
    ".github/ISSUE_TEMPLATE/feature_request.yml",
    ".github/ISSUE_TEMPLATE/config.yml",
    ".github/pull_request_template.md",
    ".github/dependabot.yml",
  ]) {
    read(publicFile);
  }

  assert.match(read("CONTRIBUTING.md"), /\.\/mvnw verify/);
  assert.match(read("SECURITY.md"), /Report a vulnerability|Private Vulnerability Report/);
  assert.match(read(".github/dependabot.yml"), /package-ecosystem: maven/);
  assert.match(read(".github/dependabot.yml"), /package-ecosystem: npm/);
  assert.match(read(".github/dependabot.yml"), /package-ecosystem: github-actions/);
});

test("public guides do not expose course authoring or internal revision language", () => {
  const publicDocuments = [
    "README.md",
    "CONTRIBUTING.md",
    "SECURITY.md",
    "SUPPORT.md",
    "CODE_OF_CONDUCT.md",
    "contracts/README.md",
    "datasets/README.md",
    "deploy/README.md",
    "labs/README.md",
    "labs/langchain4j-lab/README.md",
    "labs/spring-ai-alibaba-lab/README.md",
    "labs/agentscope-lab/README.md",
    "labs/protocol-interop-lab/README.md",
    "apps/customer-web/README.md",
    ...readdirSync(path.join(projectRoot, "docs", "runbooks"))
      .filter((name) => name.endsWith(".md"))
      .map((name) => `docs/runbooks/${name}`),
  ];

  const authoringLanguage = /默认读者|文章演示|读文章|读者(?:需要|无需|只需)|付费专栏|专栏课程/;
  for (const document of publicDocuments) {
    assert.doesNotMatch(read(document), authoringLanguage, document);
  }
});

test("public runtime documentation matches the single-YAML configuration model", () => {
  const runtimeDocuments = [
    "README.md",
    "docs/architecture/system-context.md",
    "docs/runbooks/live-model-smoke.md",
    "docs/runbooks/local-toolchain.md",
    "docs/runbooks/runtime-configuration.md",
    "docs/reports/lesson-19-grounded-rag-answer.md",
    "docs/reports/lesson-38-runtime-configuration.md",
    "services/knowledge-service/README.md",
    "services/ticket-agent-service/README.md",
    "apps/customer-bff/README.md",
  ];

  for (const document of runtimeDocuments) {
    const content = read(document);
    assert.doesNotMatch(content, /application\.example\.yml|application-rag-postgres\.yml/, document);
  }

  const readme = read("README.md");
  assert.match(readme, /spring-boot\.run\.profiles=production/);
  assert.match(readme, /config\/application\.yml/);
  assert.match(readme, /不会被上述启动命令自动加载/);
  assert.match(read("docs/runbooks/runtime-configuration.md"), /java-ai\.knowledge\.postgres\.\*/);
});

test("verification archive excludes course headings and private workspace revisions", () => {
  const reportsDirectory = path.join(projectRoot, "docs", "reports");
  const reportNames = readdirSync(reportsDirectory).filter((name) => name.endsWith(".md"));

  for (const reportName of reportNames) {
    const report = read(`docs/reports/${reportName}`);
    assert.doesNotMatch(report, /^#\s+(?:Lesson\s+\d+|第\s*\d+\s*(?:讲|篇|课)|里程碑\s*\d+)/m, reportName);
    assert.doesNotMatch(report, /Implementation commit|Input baseline commit|提交基线/, reportName);
  }
});
