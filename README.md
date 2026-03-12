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
- Build troubleshooting guide: `docs/build-troubleshooting.md`
- Build verification presets: `scripts/BUILD_PRESETS.md`
- Performance notes: `docs/performance-baseline.md`
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

## Performance Snapshot (2026-03-10)

- External-process reuse: cold 808 ms, repeated avg 2.00 ms (executor cache hits 20, misses 1)
- Isolation mode comparison (repeated avg): DIRECT 0.35 ms, ISOLATED_THREAD 0.50 ms, EXTERNAL_PROCESS 1.75 ms
- Large-context comparison (repeated avg): DIRECT 0.50 ms, ISOLATED_THREAD 0.80 ms, EXTERNAL_PROCESS 3.70 ms
- Detailed notes: `docs/performance-baseline.md` (environment-specific, use for trend comparison)

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
