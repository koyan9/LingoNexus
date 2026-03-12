# LingoNexus

<div align="center">

[![License](https://img.shields.io/badge/license-Apache%202.0-0a7ea4.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17+-0a7ea4.svg)](https://www.oracle.com/java/)
[![Docs Ready](https://img.shields.io/badge/docs-ready-0a7ea4.svg)](docs/INDEX.md)
[![Build Presets](https://img.shields.io/badge/build%20presets-ready-0a7ea4.svg)](scripts/BUILD_PRESETS.md)

**面向 Java 应用的安全、可扩展、多语言脚本执行引擎**

🌐 Language / 语言: [English](README.md) | [简体中文](README.zh-CN.md)

📚 快捷入口: [文档](docs/INDEX.md) · [构建](docs/build-troubleshooting.md) · [预设](scripts/BUILD_PRESETS.md)

</div>

## 项目概览

LingoNexus 用于在 Java 应用中嵌入动态脚本能力。当前实现已经具备统一执行门面、多语言沙箱、内置模块、隔离执行模式和运行时诊断能力。

当前版本的重点能力：

- 支持 `groovy`、`javascript`（GraalJS）、`java`、`javaexpr`、`kotlin`
- 对外统一使用 `LingoNexusExecutor`
- 支持 `AUTO`、`DIRECT`、`ISOLATED_THREAD`、`EXTERNAL_PROCESS` 四种隔离模式
- 内置 `math`、`str`、`date`、`json`、`validator`、`formatter`、`codec`、`convert`、`col` 等模块
- 提供 `EngineStatistics`、`EngineDiagnostics`、`ExternalProcessStatistics` 诊断能力

## 模块说明

| 模块 | 职责 |
| --- | --- |
| `lingonexus-api` | 公共契约、配置对象、结果类型、异常与统计接口 |
| `lingonexus-core` | Builder、执行编排、缓存、诊断、外部进程执行 |
| `lingonexus-script-*` | 各脚本语言的沙箱实现 |
| `lingonexus-utils` | 内置模块依赖的通用工具能力 |
| `lingonexus-modules` | 内置脚本模块 |
| `lingonexus-spring-boot-starter` | Spring Boot 自动配置 |
| `lingonexus-examples` | 示例代码 |
| `lingonexus-testcase` | 无 Spring / Spring Boot 场景验证 |

## 开始之前

- 新读者先看：`docs/INDEX.md`
- 想快速接入：`docs/quick-start.md`
- 想了解架构：`docs/architecture.md`
- 需要构建排障：`docs/build-troubleshooting.md`
- 需要复用验证预设：`scripts/BUILD_PRESETS.md`

如果在 Windows PowerShell 中看到中文乱码，先执行 `chcp 65001`，或用 `Get-Content -Encoding utf8 README.zh-CN.md` 按 UTF-8 显式读取文件。

## 快速开始

### Spring Boot 集成

添加依赖：

```xml
<dependency>
    <groupId>io.github.koyan9</groupId>
    <artifactId>lingonexus-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

最小配置：

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

在业务代码中注入执行器：

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

### 独立模式

将需要的模块加入 classpath：

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

创建引擎实例：

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

## 隔离模式

| 模式 | 行为 | 适用场景 |
| --- | --- | --- |
| `AUTO` | 超时开启时走隔离线程池，否则走直接执行 | 默认推荐 |
| `DIRECT` | 在调用线程中执行 | 开销最低 |
| `ISOLATED_THREAD` | 在隔离线程池中执行 | 需要超时控制的进程内执行 |
| `EXTERNAL_PROCESS` | 在持久化外部 JVM worker 池中执行 | 更强隔离与更完整的 worker 诊断 |

当前 `EXTERNAL_PROCESS` 最适合 JSON-safe 的变量、metadata 和结果值。

如果需要限制 worker 借用等待时间，可配置 `lingonexus.sandbox.external-process-borrow-timeout-ms`（默认跟随 `timeout-ms`；设为 `0` 表示不限制）。

## 内置模块

| 模块 | 示例能力 |
| --- | --- |
| `math` | `add`、`multiply`、`scale`、`pow` |
| `str` | `trim`、`split`、`join` |
| `date` | `now`、`timestamp`、`addDays` |
| `json` | `toJson`、`parsePath` |
| `validator` | `isEmail`、`isPhone`、`range` |
| `formatter` | `currency`、`percent`、`mask` |
| `codec` | `base64Encode`、`md5`、`uuid` |
| `convert` | 类型转换辅助 |
| `col` | 集合检查与转换 |

## 参考文档与诊断

- 当前架构基线：`docs/architecture.md`
- 诊断说明：`docs/diagnostics.md`
- 构建与排障说明：`docs/build-troubleshooting.md`
- 构建验证预设：`scripts/BUILD_PRESETS.md`
- 性能基线：`docs/performance-baseline.md`
- 压测脚本 / 报告生成：`scripts/run-performance-baselines.ps1`（按需生成 `docs/performance-reports/` 下的报告）
- 后续计划：`docs/todo-plan.md`
- 当前项目状态：`docs/project-status.md`

公共执行器还提供：

- `engine.getStatistics()`：获取聚合执行统计
- `engine.getDiagnostics()`：获取缓存、线程池、worker 池等运行时诊断信息

当外部进程的 payload 兼容性校验失败时，`ScriptResult.metadata` 可能包含结构化诊断字段：
`errorPath`、`errorValueType`、`errorDetailReason`。

外部进程失败会产生稳定的 `errorReason` 码，例如
`worker_borrow_timeout`、`worker_startup_failed`、`worker_borrow_interrupted`、`worker_execution_timeout`。

`engine.getDiagnostics().getExternalProcessStatistics()` 还提供 `borrowTimeoutCount` 用于观察 worker 借用超时情况。

## 性能快照（2026-03-10）

- 外部进程复用：冷启动 808 ms，重复均值 2.00 ms（executor cache hits 20，misses 1）
- 隔离模式对比（重复均值）：DIRECT 0.35 ms，ISOLATED_THREAD 0.50 ms，EXTERNAL_PROCESS 1.75 ms
- 大上下文对比（重复均值）：DIRECT 0.50 ms，ISOLATED_THREAD 0.80 ms，EXTERNAL_PROCESS 3.70 ms
- 详细说明：`docs/performance-baseline.md`（与环境相关，用于趋势对比）

## 示例代码

示例位于 `lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/`：

- `QuickStartExample.java`
- `MultiLanguageExample.java`
- `BusinessModulesExample.java`
- `DiagnosticsExample.java`
- `LingoNexusContextExample.java`

## 构建与验证

```bash
mvn clean compile
mvn test
mvn test -pl lingonexus-core
mvn clean package -DskipTests
```

## 老文档迁移提示

如果你是从旧文档或旧示例迁移：

- 请使用 `LingoNexusExecutor`，不要再使用 `ScriptEngineFacade`
- JavaScript 当前是 `GraalJSSandbox` / `javascript`，不是旧的 Rhino 表述
- Spring Boot 配置键使用 `timeout-ms`、`expire-after-write-ms`、`expire-after-access-ms`
- 独立模式优先使用 `LingoNexusBuilder.createNewInstance(...)` 或 `LingoNexusBuilder.loadInstance(...)`

## License
