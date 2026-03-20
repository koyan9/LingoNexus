# LingoNexus

<div align="center">

[![License](https://img.shields.io/badge/license-Apache%202.0-0a7ea4.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17+-0a7ea4.svg)](https://www.oracle.com/java/)
[![Docs Ready](https://img.shields.io/badge/docs-ready-0a7ea4.svg)](docs/INDEX.md)
[![Build Presets](https://img.shields.io/badge/build%20presets-ready-0a7ea4.svg)](scripts/BUILD_PRESETS.md)

**A secure, extensible, multi-language script execution engine for Java applications**

🌐 Language / 语言: [English](README.md) | [简体中文](README.zh-CN.md)

📚 Quick Links: [Docs](docs/INDEX.md) · [Build](docs/build-troubleshooting.md) · [Presets](scripts/BUILD_PRESETS.md)

</div>

## Overview

LingoNexus embeds dynamic scripting into Java applications with a unified execution surface, pluggable language sandboxes, built-in utility modules, and multiple isolation modes.

Current implementation highlights:

- Supports `groovy`, `javascript` (GraalJS), `java`, `javaexpr`, and `kotlin`
- Uses `LingoNexusExecutor` as the public execution facade
- Supports `AUTO`, `DIRECT`, `ISOLATED_THREAD`, and `EXTERNAL_PROCESS` isolation modes
- Provides built-in modules such as `math`, `str`, `date`, `json`, `validator`, `formatter`, `codec`, `convert`, and `col`
- Exposes runtime diagnostics through `EngineStatistics`, `EngineDiagnostics`, and `ExternalProcessStatistics`

## Modules

| Module | Responsibility |
| --- | --- |
| `lingonexus-api` | Public contracts, config types, results, exceptions, statistics |
| `lingonexus-core` | Builder, orchestration, execution, cache, diagnostics, external-process runtime |
| `lingonexus-script-*` | Language-specific sandbox implementations |
| `lingonexus-utils` | Shared utility support used by built-in modules |
| `lingonexus-modules` | Built-in script modules |
| `lingonexus-spring-boot-starter` | Spring Boot auto-configuration |
| `lingonexus-examples` | Usage examples |
| `lingonexus-testcase` | No-Spring and Spring Boot verification tests |

## Start Here

- New to the repo: `docs/INDEX.md`
- Integrating quickly: `docs/quick-start.md`
- Understanding architecture: `docs/architecture.md`
- Triaging failures: `docs/diagnostics.md`
- Tracking performance baselines: `docs/performance-baseline.md`
- Troubleshooting local builds: `docs/build-troubleshooting.md`
- Reusing verification presets: `scripts/BUILD_PRESETS.md`

If Chinese text looks garbled in Windows PowerShell, first try `chcp 65001`, or read the file explicitly as UTF-8 with `Get-Content -Encoding utf8 README.zh-CN.md`.

## Quick Start

### Spring Boot

Add the starter:

```xml
<dependency>
    <groupId>io.github.koyan9</groupId>
    <artifactId>lingonexus-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Minimal configuration:

```yaml
lingonexus:
  enabled: true
  default-language: groovy
  cache:
    enabled: true
    max-size: 1000
    expire-after-write-ms: 3600000
    expire-after-access-ms: 1800000
  sandbox:
    enabled: true
    max-script-size: 65536
    timeout-ms: 5000
    isolation-mode: AUTO
```

Use the executor bean:

```java
@RestController
@RequestMapping("/scripts")
public class ScriptController {

    private final LingoNexusExecutor lingoNexusExecutor;

    public ScriptController(LingoNexusExecutor lingoNexusExecutor) {
        this.lingoNexusExecutor = lingoNexusExecutor;
    }

    @PostMapping("/execute")
    public ScriptResult execute(@RequestBody ScriptRequest request) {
        ScriptContext context = ScriptContext.builder()
                .put("value", request.getValue())
                .build();

        return lingoNexusExecutor.execute(
                request.getScript(),
                request.getLanguage(),
                context
        );
    }
}
```

### Standalone

Add the modules you need to the classpath:

```xml
<dependency>
    <groupId>io.github.koyan9</groupId>
    <artifactId>lingonexus-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.koyan9</groupId>
    <artifactId>lingonexus-script-groovy</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
<dependency>
    <groupId>io.github.koyan9</groupId>
    <artifactId>lingonexus-modules</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Create an engine instance:

```java
LingoNexusConfig config = LingoNexusConfig.builder()
        .defaultLanguage(ScriptLanguage.GROOVY)
        .sandboxConfig(SandboxConfig.builder()
                .enabled(true)
                .timeoutMs(5000)
                .build())
        .build();

LingoNexusExecutor engine = LingoNexusBuilder.createNewInstance(config);

ScriptContext context = ScriptContext.builder()
        .put("price", 299.99)
        .put("quantity", 5)
        .build();

ScriptResult result = engine.execute(
        "math.multiply(price, quantity)",
        "groovy",
        context
);
```

## Usage Guide

LingoNexus centers around the `LingoNexusExecutor` facade. It supports synchronous, asynchronous, and batch execution with one API surface.

### Execution APIs

| Method | Returns | Notes |
| --- | --- | --- |
| `execute(script, language, context)` | `ScriptResult` | Synchronous execution, throws on security/runtime failures. |
| `executeAsync(script, language, context)` | `CompletableFuture<ScriptResult>` | Uses the async executor pool. |
| `executeBatch(scripts, language, context)` | `List<ScriptResult>` | Batch execution for a single language + shared context. |

### Script Context and Metadata

`ScriptContext` carries variables and request metadata into the sandbox.

```java
ScriptContext context = ScriptContext.builder()
        .put("price", 299.99)
        .put("quantity", 5)
        .putMetadata("requestId", "req-20260310-001")
        .putMetadata("timeoutOverride", 2000)
        .build();
```

Metadata keys are free-form, but common keys are centralized in
`io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys`.

### Modules and Global Variables

Modules are exposed by name (for example `math`, `str`, `json`). You can filter what is available using:

- `lingonexus.exclude-script-modules` (higher priority)
- `lingonexus.allowed-script-modules`

Global variables are configured once in Spring Boot and merged into every request context:

```yaml
lingonexus:
  global-variables:
    appName: "billing-service"
    env: "prod"
```

### Diagnostics and Statistics

`engine.getStatistics()` and `engine.getDiagnostics()` expose runtime counters and pool/cache diagnostics.
For per-request metadata, enable `lingonexus.metadata.*` (see configuration reference below).

Recommended reading order when diagnosing external-process problems:

- `ScriptResult.metadata.errorStage` / `errorComponent` / `errorReason`
- `docs/diagnostics.md` for the matching triage path
- `engine.getDiagnostics().getExternalProcessStatistics()` for latest failure snapshots and aggregated counts

## Configuration Reference (Spring Boot)

The tables below reflect `LingoNexusProperties` defaults. Ranges and constraints are enforced by the config
builders where noted.

### Core

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.enabled` | boolean | `true` | `true` / `false` | `false` disables auto-configuration. |
| `lingonexus.default-language` | string | `groovy` | `groovy`, `javascript`, `java`, `javaexpr`, `kotlin` | Aliases accepted (for example `js`, `ecmascript`, `janino`). |

### Cache

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.cache.enabled` | boolean | `true` | `true` / `false` | Enables ScriptCacheManager (compiled script cache). |
| `lingonexus.cache.max-size` | int | `1000` | `>= 0` recommended | Max cache entries. |
| `lingonexus.cache.expire-after-write-ms` | long | `3600000` | `>= 0` recommended | Expire after write (ms). |
| `lingonexus.cache.expire-after-access-ms` | long | `1800000` | `>= 0` recommended | Expire after access (ms). |

### Sandbox and Isolation

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.sandbox.enabled` | boolean | `true` | `true` / `false` | Enables sandbox checks. |
| `lingonexus.sandbox.max-script-size` | int | `65536` | `>= 1` recommended | Script size in bytes. |
| `lingonexus.sandbox.timeout-ms` | long | `5000` | `>= 0` | `0` disables timeout; `AUTO` then uses `DIRECT`. |
| `lingonexus.sandbox.enable-engine-cache` | boolean | `true` | `true` / `false` | Only affects JavaExpr (liquor-eval). |
| `lingonexus.sandbox.isolation-mode` | string | `AUTO` | `AUTO`, `DIRECT`, `ISOLATED_THREAD`, `EXTERNAL_PROCESS` | Default is safe auto routing. |
| `lingonexus.sandbox.class-whitelist` | list | empty | wildcard patterns | Allowed classes (optional). |
| `lingonexus.sandbox.class-blacklist` | list | empty | wildcard patterns | Denied classes, higher priority than whitelist. |
| `lingonexus.sandbox.external-process-pool-size` | int | `1` | `> 0` | Max external workers. |
| `lingonexus.sandbox.external-process-startup-retries` | int | `2` | `>= 0` | Retry count for worker startup. |
| `lingonexus.sandbox.external-process-prewarm-count` | int | `1` | `>= 0` | Workers to prewarm on boot. |
| `lingonexus.sandbox.external-process-idle-ttl-ms` | long | `300000` | `>= 0` | Idle eviction TTL. |
| `lingonexus.sandbox.external-process-borrow-timeout-ms` | long | `-1` | `-1` or `>= 0` | `-1` follows `timeout-ms`, `0` disables the cap. |
| `lingonexus.sandbox.external-process-executor-cache-max-size` | int | `8` | `> 0` | Per-worker executor cache size. |
| `lingonexus.sandbox.external-process-executor-cache-idle-ttl-ms` | long | `300000` | `>= 0` | Idle eviction for per-worker executor cache. |

### Sandbox Selection (Advanced)

These keys filter which sandbox providers are eligible during SPI discovery.

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.allowed-sandbox-implementations` | list | empty | sandbox class FQCN | Allow only specific sandbox implementations. |
| `lingonexus.allowed-sandbox-languages` | list | empty | language IDs | Allow only specific language IDs. |
| `lingonexus.allowed-sandbox-host-access-modes` | list | empty | `JVM_CLASSLOADER`, `POLYGLOT_HOST`, `BYTECODE_COMPILER`, `SCRIPTING_HOST`, `EXPRESSION_ENGINE` | Filter by host access model. |
| `lingonexus.allowed-sandbox-host-restriction-modes` | list | empty | `UNSPECIFIED`, `STRICT`, `MODERATE`, `RELAXED` | Filter by restriction strength. |
| `lingonexus.required-sandbox-host-restriction-flags` | list | empty | `REFLECTION_BLOCKED`, `FILE_IO_BLOCKED`, `NETWORK_BLOCKED` | Require specific restriction capabilities. |
| `lingonexus.allowed-sandbox-result-transport-modes` | list | empty | `ANY`, `JSON_SAFE_RESULT`, `JSON_SAFE_RESULT_AND_METADATA` | Filter by JSON-safe result support. |
| `lingonexus.allowed-sandbox-transport-serializer-modes` | list | empty | `JSON_FRAMED`, `BINARY_FRIENDLY`, `CUSTOM_SERIALIZER_REQUIRED` | Filter by serializer model. |
| `lingonexus.allowed-sandbox-transport-payload-profiles` | list | empty | `SMALL_PAYLOAD_ONLY`, `STANDARD_PAYLOAD`, `LARGE_PAYLOAD_FRIENDLY` | Filter by payload profile. |
| `lingonexus.required-sandbox-transport-protocol-capabilities` | list | empty | `JSON_FRAMED`, `CBOR_CAPABLE`, `STREAMING_CAPABLE`, `CUSTOM_SERIALIZER_CONTRACT` | Require protocol capabilities. |
| `lingonexus.required-sandbox-transport-serializer-contract-ids` | list | empty | string IDs | Require serializer contract IDs. |
| `lingonexus.require-engine-cache-capable-sandbox` | boolean | `false` | `true` / `false` | Require engine-internal cache support. |
| `lingonexus.require-external-process-compatible-sandbox` | boolean | `false` | `true` / `false` | Require external-process compatibility. |
| `lingonexus.require-json-safe-external-result` | boolean | `false` | `true` / `false` | Require JSON-safe external results. |
| `lingonexus.require-json-safe-external-metadata` | boolean | `false` | `true` / `false` | Require JSON-safe external metadata. |

Example:

```yaml
lingonexus:
  allowed-sandbox-languages:
    - groovy
    - javascript
  allowed-sandbox-host-restriction-modes:
    - STRICT
  required-sandbox-host-restriction-flags:
    - REFLECTION_BLOCKED
    - FILE_IO_BLOCKED
  require-external-process-compatible-sandbox: true
  require-json-safe-external-result: true
```

### Executor (Async)

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.executor.core-pool-size` | int | `CPU * 2` | `> 0` | Core async threads. |
| `lingonexus.executor.max-pool-size` | int | `max(CPU * 4, 50)` | `>= core-pool-size` | Max async threads. |
| `lingonexus.executor.keep-alive-time-seconds` | long | `60` | `>= 0` | Thread keep-alive. |
| `lingonexus.executor.queue-capacity` | int | `1000` | `> 0` | Task queue size. |
| `lingonexus.executor.thread-name-prefix` | string | `ScriptExecutor-Async-` | non-empty | Thread name prefix. |
| `lingonexus.executor.rejection-policy` | string | `CALLER_RUNS` | `CALLER_RUNS`, `ABORT`, `DISCARD`, `DISCARD_OLDEST` | Rejection strategy. |

### Executor (Isolated)

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.isolated-executor.core-pool-size` | int | `32` | `> 0` | Threads for `ISOLATED_THREAD`. |
| `lingonexus.isolated-executor.max-pool-size` | int | `256` | `>= core-pool-size` | Max isolated threads. |
| `lingonexus.isolated-executor.keep-alive-time-seconds` | long | `60` | `>= 0` | Thread keep-alive. |
| `lingonexus.isolated-executor.queue-capacity` | int | `2000` | `> 0` | Task queue size. |
| `lingonexus.isolated-executor.thread-name-prefix` | string | `ScriptExecutor-Isolated-` | non-empty | Thread name prefix. |
| `lingonexus.isolated-executor.rejection-policy` | string | `CALLER_RUNS` | `CALLER_RUNS`, `ABORT`, `DISCARD`, `DISCARD_OLDEST` | Rejection strategy. |

### Modules and Globals

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.exclude-script-modules` | list | empty | module name / class / wildcard | Highest priority filter. |
| `lingonexus.allowed-script-modules` | list | empty | module name / class / wildcard | Allow-list; empty means no allow filter. |
| `lingonexus.global-variables` | map | empty | string keys | Merged into every `ScriptContext`. |

### Metadata Policies

| Key | Type | Default | Range / Valid | Notes |
| --- | --- | --- | --- | --- |
| `lingonexus.metadata.profile` | string | `FULL` | `BASIC`, `TIMING`, `FULL` | Used when no policy/categories override it. |
| `lingonexus.metadata.policy` | string | unset | `MINIMAL`, `TIMING`, `DEBUG` | Built-in policy presets. |
| `lingonexus.metadata.policy-name` | string | unset | template name | Matches built-in policies or configured templates. |
| `lingonexus.metadata.categories` | list | empty | `TIMING`, `THREAD`, `MODULE`, `SECURITY`, `ERROR_DIAGNOSTICS` | If set, overrides policy/profile. |
| `lingonexus.metadata.policy-templates.<name>.parent-policy-name` | string | unset | built-in or template name | Inherits categories from parent. |
| `lingonexus.metadata.policy-templates.<name>.categories` | list | empty | category names | Adds categories to the template. |

Metadata precedence is: `categories` > `policy-name` > `policy` > `profile`.
Setting `metadata.categories` to an empty list disables detailed metadata output.

### Advanced System Properties

| Key | Default | Notes |
| --- | --- | --- |
| `-Dlingonexus.externalProcess.maxFrameBytes` | `67108864` | Cap external-process protocol frames (bytes). |

### Full Configuration Example

```yaml
lingonexus:
  enabled: true
  default-language: groovy
  cache:
    enabled: true
    max-size: 1000
    expire-after-write-ms: 3600000
    expire-after-access-ms: 1800000
  sandbox:
    enabled: true
    max-script-size: 65536
    timeout-ms: 5000
    enable-engine-cache: true
    isolation-mode: AUTO
    class-whitelist:
      - "java.lang.*"
      - "java.util.*"
    class-blacklist:
      - "java.io.*"
      - "java.lang.reflect.*"
    external-process-pool-size: 1
    external-process-startup-retries: 2
    external-process-prewarm-count: 1
    external-process-idle-ttl-ms: 300000
    external-process-borrow-timeout-ms: -1
    external-process-executor-cache-max-size: 8
    external-process-executor-cache-idle-ttl-ms: 300000
  executor:
    core-pool-size: 8
    max-pool-size: 64
    keep-alive-time-seconds: 60
    queue-capacity: 1000
    thread-name-prefix: ScriptExecutor-Async-
    rejection-policy: CALLER_RUNS
  isolated-executor:
    core-pool-size: 32
    max-pool-size: 256
    keep-alive-time-seconds: 60
    queue-capacity: 2000
    thread-name-prefix: ScriptExecutor-Isolated-
    rejection-policy: CALLER_RUNS
  exclude-script-modules:
    - "io.github.koyan9.lingonexus.utils.MathModule"
    - "json"
  allowed-script-modules:
    - "io.github.koyan9.lingonexus.utils.*"
    - "math"
  metadata:
    profile: TIMING
    policy: TIMING
    policy-name: teamTiming
    categories:
      - TIMING
      - THREAD
    policy-templates:
      teamTiming:
        parent-policy-name: timing
        categories:
          - MODULE
  global-variables:
    appName: "billing-service"
    version: "1.0.0"
```

### Standalone Builder Reference (Advanced)

Spring Boot exposes a curated subset of configuration. For standalone usage, `LingoNexusConfig.Builder`
offers additional sandbox selection and transport filters.

| Builder API | Values / Purpose |
| --- | --- |
| `allowedSandboxImplementation(...)` | Allow only specific sandbox class names (FQCN). |
| `allowedSandboxLanguage(...)` | Allow only specific language IDs (for example `groovy`, `javascript`). |
| `allowedSandboxHostAccessMode(...)` | Filter by host access model. |
| `allowedSandboxHostRestrictionMode(...)` | Filter by restriction strength. |
| `requireSandboxHostRestrictionFlag(...)` | Require specific restriction capabilities. |
| `allowedSandboxResultTransportMode(...)` | Filter by JSON-safe transport support. |
| `allowedSandboxTransportSerializerMode(...)` | Filter by serializer model. |
| `allowedSandboxTransportPayloadProfile(...)` | Filter by payload profile. |
| `requireSandboxTransportProtocolCapability(...)` | Require protocol-level capabilities. |
| `requireSandboxTransportSerializerContractId(...)` | Require custom serializer contract IDs. |
| `requireEngineCacheCapableSandbox(true/false)` | Require engine-internal cache support. |
| `requireExternalProcessCompatibleSandbox(true/false)` | Require external-process compatibility. |
| `requireJsonSafeExternalResult(true/false)` | Enforce JSON-safe results in external mode. |
| `requireJsonSafeExternalMetadata(true/false)` | Enforce JSON-safe metadata in external mode. |

Enum value sets:

| Enum | Values |
| --- | --- |
| `SandboxHostAccessMode` | `JVM_CLASSLOADER`, `POLYGLOT_HOST`, `BYTECODE_COMPILER`, `SCRIPTING_HOST`, `EXPRESSION_ENGINE` |
| `SandboxHostRestrictionMode` | `UNSPECIFIED`, `STRICT`, `MODERATE`, `RELAXED` |
| `SandboxHostRestrictionFlag` | `REFLECTION_BLOCKED`, `FILE_IO_BLOCKED`, `NETWORK_BLOCKED` |
| `SandboxResultTransportMode` | `ANY`, `JSON_SAFE_RESULT`, `JSON_SAFE_RESULT_AND_METADATA` |
| `SandboxTransportSerializerMode` | `JSON_FRAMED`, `BINARY_FRIENDLY`, `CUSTOM_SERIALIZER_REQUIRED` |
| `SandboxTransportPayloadProfile` | `SMALL_PAYLOAD_ONLY`, `STANDARD_PAYLOAD`, `LARGE_PAYLOAD_FRIENDLY` |
| `SandboxTransportProtocolCapability` | `JSON_FRAMED`, `CBOR_CAPABLE`, `STREAMING_CAPABLE`, `CUSTOM_SERIALIZER_CONTRACT` |

## Isolation Modes

| Mode | Behavior | Good for |
| --- | --- | --- |
| `AUTO` | Uses isolated-thread execution when timeout is enabled, otherwise direct execution | Safe default |
| `DIRECT` | Runs on the caller thread | Lowest overhead |
| `ISOLATED_THREAD` | Runs on the isolated executor pool | Timeout control and safer in-process execution |
| `EXTERNAL_PROCESS` | Runs inside a persistent external JVM worker pool | Stronger isolation and worker-pool diagnostics |

`EXTERNAL_PROCESS` currently works best with JSON-safe variables, metadata, and result values.

If external workers are saturated, set `lingonexus.sandbox.external-process-borrow-timeout-ms` to cap borrow wait time (defaults to `timeout-ms`; set `0` to disable the cap).

## Built-in Modules

| Module | Examples |
| --- | --- |
| `math` | `add`, `multiply`, `scale`, `pow` |
| `str` | `trim`, `split`, `join` |
| `date` | `now`, `timestamp`, `addDays` |
| `json` | `toJson`, `parsePath` |
| `validator` | `isEmail`, `isPhone`, `range` |
| `formatter` | `currency`, `percent`, `mask` |
| `codec` | `base64Encode`, `md5`, `uuid` |
| `convert` | type conversion helpers |
| `col` | collection inspection and transformation |

## Reference and Diagnostics

- Current architecture baseline: `docs/architecture.md`
- Runtime diagnostics guide: `docs/diagnostics.md`
- Diagnostics example: `lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/DiagnosticsExample.java`
- Build troubleshooting guide: `docs/build-troubleshooting.md`
- Build verification presets: `scripts/BUILD_PRESETS.md`
- Performance notes: `docs/performance-baseline.md`
- Performance report index: `docs/performance-reports/INDEX.md`
- Benchmark / report workflow: `scripts/run-performance-baselines.ps1` (generates reports under `docs/performance-reports/` when needed)
- Follow-up work: `docs/todo-plan.md`
- Current project status: `docs/project-status.md`

The public executor surface also exposes:

- `engine.getStatistics()` for aggregated execution counters
- `engine.getDiagnostics()` for cache, thread-pool, and worker-pool state

When external-process payload compatibility fails, `ScriptResult.metadata` may include structured diagnostics:
`errorPath`, `errorValueType`, and `errorDetailReason`.

External-process failures are classified using stable `errorReason` codes such as
`worker_borrow_timeout`, `worker_startup_failed`, `worker_borrow_interrupted`, and `worker_execution_timeout`.

`engine.getDiagnostics().getExternalProcessStatistics()` also includes `borrowTimeoutCount` to track pool saturation.

## Performance Snapshot (2026-03-19)

- External-process reuse: cold 770 ms, repeated avg 2.00 ms (executor cache hits 20, misses 1)
- Isolation mode comparison (repeated avg): DIRECT 0.40 ms, ISOLATED_THREAD 0.60 ms, EXTERNAL_PROCESS 2.45 ms
- Large-context comparison (repeated avg): DIRECT 0.20 ms, ISOLATED_THREAD 0.50 ms, EXTERNAL_PROCESS 4.00 ms
- Direct-path metadata profile comparison (repeated avg): BASIC 0.10 ms, TIMING 0.05 ms, FULL 0.10 ms
- Direct-path context-source comparison (repeated avg): global-heavy 0.50 ms, request-heavy 0.30 ms
- Direct-path module-usage comparison (repeated avg): simple arithmetic 0.05 ms, module-heavy 0.40 ms
- Direct-path failure-diagnostics comparison (repeated avg): success-full 0.05 ms, error-diagnostics-only 0.00 ms, failure-full 0.00 ms
- Detailed notes and latest generated report: `docs/performance-baseline.md`, `docs/performance-reports/INDEX.md`

## Examples

See `lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/`:

- `QuickStartExample.java`
- `MultiLanguageExample.java`
- `BusinessModulesExample.java`
- `DiagnosticsExample.java`
- `LingoNexusContextExample.java`

## Build and Verify

```bash
mvn clean compile
mvn test
mvn test -pl lingonexus-core
mvn clean package -DskipTests
```

## Notes for Existing Users

If you are updating older integration code or older docs:

- Use `LingoNexusExecutor` instead of `ScriptEngineFacade`
- Use `GraalJSSandbox` / `javascript` instead of old Rhino-based wording
- Use `timeout-ms`, `expire-after-write-ms`, and `expire-after-access-ms` in Spring Boot config
- Prefer `LingoNexusBuilder.createNewInstance(...)` or `LingoNexusBuilder.loadInstance(...)` over old fluent-builder examples

## License

Apache License 2.0. See `LICENSE`.
