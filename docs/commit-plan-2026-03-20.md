# Commit Plan (2026-03-20)

> Purpose: split the current working tree into reviewable, low-conflict commit batches.

## Batch 1: Spring Boot Config Surface

Goal:

- expose the newer sandbox-selection and transport-related options through Spring Boot binding
- keep config-facing changes together with their config tests

Primary files:

- `lingonexus-spring-boot-starter/src/main/java/io/github/koyan9/lingonexus/springboot/LingoNexusProperties.java`
- `lingonexus-spring-boot-starter/src/main/java/io/github/koyan9/lingonexus/springboot/LingoNexusAutoConfiguration.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/core/config/ConfigFeatureTest.java`
- `lingonexus-core/src/main/java/io/github/koyan9/lingonexus/core/security/ScriptSizePolicy.java`

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-springboot -am test -Dsurefire.failIfNoSpecifiedTests=false
```

## Batch 2: External-Process Diagnostics Hardening

Goal:

- make request/handshake/borrow/worker failures stay visible through stable diagnostics snapshots and counts
- keep low-level executor changes with facade-level regression coverage and diagnostics example updates

Primary files:

- `lingonexus-core/src/main/java/io/github/koyan9/lingonexus/core/process/ExternalProcessScriptExecutor.java`
- `lingonexus-core/src/main/java/io/github/koyan9/lingonexus/core/impl/DefaultScriptExecutor.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/core/process/ExternalProcessScriptExecutorFeatureTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ExternalProcessCompatibilityTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ExternalProcessIsolationTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/EngineDiagnosticsFeatureTest.java`
- `lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/DiagnosticsExample.java`

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=ExternalProcessCompatibilityTest,EngineDiagnosticsFeatureTest,ProtocolNegotiationFeatureTest,ExternalProcessIsolationTest,ExternalProcessScriptExecutorFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Batch 3: Direct-Path Hot-Path Reduction

Goal:

- reduce avoidable merge/copy and metadata-plan derivation on the direct execution path
- keep hot-path code changes with the direct-path metadata and preparation regressions

Primary files:

- `lingonexus-core/src/main/java/io/github/koyan9/lingonexus/core/executor/ExecutionPreparationService.java`
- `lingonexus-core/src/main/java/io/github/koyan9/lingonexus/core/impl/DefaultScriptExecutor.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ExecutionPreparationServiceFeatureTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ResultMetadataProfileFeatureTest.java`

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am -Dtest=ExecutionPreparationServiceFeatureTest,DefaultScriptExecutorInProcessFeatureTest,ResultMetadataProfileFeatureTest,ResultMetadataCategoryFeatureTest,ResultMetadataPolicyFeatureTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## Batch 4: Benchmarks and Reader-Path Docs

Goal:

- keep the new performance baselines, report script updates, and reader-path documentation in one reviewable batch
- update release/status summaries together so docs reflect the verified tree

Primary files:

- `scripts/run-performance-baselines.ps1`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ResultMetadataProfilePerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ContextSourceDistributionPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/ModuleUsageComplexityPerformanceBaselineTest.java`
- `lingonexus-testcase/lingonexus-testcase-nospring/src/test/java/io/github/koyan9/lingonexus/testcase/nospring/FailureDiagnosticsPerformanceBaselineTest.java`
- `README.md`
- `README.zh-CN.md`
- `docs/INDEX.md`
- `docs/quick-start.md`
- `docs/build-troubleshooting.md`
- `docs/diagnostics.md`
- `docs/project-status.md`
- `docs/todo-plan.md`
- `docs/milestone-m1-external-process-stability.md`
- `docs/milestone-m2-diagnostics-clarity.md`
- `docs/milestone-m3-hot-path-optimization.md`
- `docs/milestone-m4-performance-hardening.md`
- `docs/performance-baseline.md`
- `docs/performance-reports/INDEX.md`
- `docs/release-notes-2026-03-20.md`
- `docs/external-summary-2026-03-20.md`
- `.github/issue-drafts/m4/01-performance-baseline-extension.md`

Recommended validation:

```bash
mvn -q -pl lingonexus-testcase/lingonexus-testcase-nospring -am test -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl lingonexus-testcase/lingonexus-testcase-springboot -am test -Dsurefire.failIfNoSpecifiedTests=false
mvn -q -pl lingonexus-examples -am -DskipTests compile
powershell.exe -ExecutionPolicy Bypass -File scripts/run-performance-baselines.ps1 -SkipVerifiedBuild
```

## Wide Verification

The current working tree has also passed:

```bash
mvn -q test -Dsurefire.failIfNoSpecifiedTests=false
```

Use that as the final confirmation step after the smaller batches above.
