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
- 想先排查故障：`docs/diagnostics.md`
- 想先看性能基线：`docs/performance-baseline.md`
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

## 使用说明

LingoNexus 以 `LingoNexusExecutor` 为统一门面，支持同步、异步与批量执行。

### 执行入口

| 方法 | 返回值 | 说明 |
| --- | --- | --- |
| `execute(script, language, context)` | `ScriptResult` | 同步执行，安全/运行异常会抛出。 |
| `executeAsync(script, language, context)` | `CompletableFuture<ScriptResult>` | 使用异步线程池执行。 |
| `executeBatch(scripts, language, context)` | `List<ScriptResult>` | 同语言 + 同上下文的批量执行。 |

### ScriptContext 与元数据

`ScriptContext` 同时承载变量和请求元数据。

```java
ScriptContext context = ScriptContext.builder()
        .put("price", 299.99)
        .put("quantity", 5)
        .putMetadata("requestId", "req-20260310-001")
        .putMetadata("timeoutOverride", 2000)
        .build();
```

常用元数据键集中在 `io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys`。

### 模块与全局变量

模块以名称暴露（例如 `math`、`str`、`json`），可通过以下配置过滤：

- `lingonexus.exclude-script-modules`（更高优先级）
- `lingonexus.allowed-script-modules`

全局变量可在 Spring Boot 中配置，并会合并进每次请求的上下文：

```yaml
lingonexus:
  global-variables:
    appName: "billing-service"
    env: "prod"
```

### 诊断与统计

`engine.getStatistics()` 与 `engine.getDiagnostics()` 提供统计与运行时诊断。
脚本级别的元数据输出需要启用 `lingonexus.metadata.*`（见下方配置参考）。

排查外部进程问题时，推荐按这个顺序看：

- 先看 `ScriptResult.metadata.errorStage` / `errorComponent` / `errorReason`
- 再看 `docs/diagnostics.md` 对应的排障路径
- 最后结合 `engine.getDiagnostics().getExternalProcessStatistics()` 看 latest failure snapshot 和聚合计数

## 配置参考（Spring Boot）

以下表格对应 `LingoNexusProperties` 默认值，取值范围与约束以配置 Builder 的校验为准。

### 核心

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.enabled` | boolean | `true` | `true` / `false` | `false` 将关闭自动配置。 |
| `lingonexus.default-language` | string | `groovy` | `groovy`、`javascript`、`java`、`javaexpr`、`kotlin` | 支持别名（如 `js`、`ecmascript`、`janino`）。 |

### 缓存

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.cache.enabled` | boolean | `true` | `true` / `false` | 启用 ScriptCacheManager（编译缓存）。 |
| `lingonexus.cache.max-size` | int | `1000` | 建议 `>= 0` | 缓存最大条目数。 |
| `lingonexus.cache.expire-after-write-ms` | long | `3600000` | 建议 `>= 0` | 写入后过期（毫秒）。 |
| `lingonexus.cache.expire-after-access-ms` | long | `1800000` | 建议 `>= 0` | 访问后过期（毫秒）。 |

### 沙箱与隔离

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.sandbox.enabled` | boolean | `true` | `true` / `false` | 是否启用沙箱校验。 |
| `lingonexus.sandbox.max-script-size` | int | `65536` | 建议 `>= 1` | 脚本大小（字节）。 |
| `lingonexus.sandbox.timeout-ms` | long | `5000` | `>= 0` | `0` 表示不超时；`AUTO` 会走 `DIRECT`。 |
| `lingonexus.sandbox.enable-engine-cache` | boolean | `true` | `true` / `false` | 仅对 JavaExpr（liquor-eval）有效。 |
| `lingonexus.sandbox.isolation-mode` | string | `AUTO` | `AUTO`、`DIRECT`、`ISOLATED_THREAD`、`EXTERNAL_PROCESS` | 默认自动路由。 |
| `lingonexus.sandbox.class-whitelist` | list | 空 | 通配符 | 允许访问的类。 |
| `lingonexus.sandbox.class-blacklist` | list | 空 | 通配符 | 禁止访问的类，高于白名单优先级。 |
| `lingonexus.sandbox.external-process-pool-size` | int | `1` | `> 0` | 外部 worker 数量上限。 |
| `lingonexus.sandbox.external-process-startup-retries` | int | `2` | `>= 0` | Worker 启动重试次数。 |
| `lingonexus.sandbox.external-process-prewarm-count` | int | `1` | `>= 0` | 预热 worker 数量。 |
| `lingonexus.sandbox.external-process-idle-ttl-ms` | long | `300000` | `>= 0` | 空闲淘汰 TTL。 |
| `lingonexus.sandbox.external-process-borrow-timeout-ms` | long | `-1` | `-1` 或 `>= 0` | `-1` 跟随 `timeout-ms`，`0` 表示不限制。 |
| `lingonexus.sandbox.external-process-executor-cache-max-size` | int | `8` | `> 0` | Worker 侧执行器缓存上限。 |
| `lingonexus.sandbox.external-process-executor-cache-idle-ttl-ms` | long | `300000` | `>= 0` | Worker 侧执行器缓存 TTL。 |

### 沙箱筛选（高级）

以下配置用于筛选 SPI 发现的沙箱实现。

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.allowed-sandbox-implementations` | list | 空 | 沙箱类全限定名 | 仅允许指定沙箱实现。 |
| `lingonexus.allowed-sandbox-languages` | list | 空 | 语言 ID | 仅允许指定语言。 |
| `lingonexus.allowed-sandbox-host-access-modes` | list | 空 | `JVM_CLASSLOADER`、`POLYGLOT_HOST`、`BYTECODE_COMPILER`、`SCRIPTING_HOST`、`EXPRESSION_ENGINE` | 按宿主访问模型过滤。 |
| `lingonexus.allowed-sandbox-host-restriction-modes` | list | 空 | `UNSPECIFIED`、`STRICT`、`MODERATE`、`RELAXED` | 按限制强度过滤。 |
| `lingonexus.required-sandbox-host-restriction-flags` | list | 空 | `REFLECTION_BLOCKED`、`FILE_IO_BLOCKED`、`NETWORK_BLOCKED` | 要求限制能力。 |
| `lingonexus.allowed-sandbox-result-transport-modes` | list | 空 | `ANY`、`JSON_SAFE_RESULT`、`JSON_SAFE_RESULT_AND_METADATA` | 按 JSON-safe 结果能力过滤。 |
| `lingonexus.allowed-sandbox-transport-serializer-modes` | list | 空 | `JSON_FRAMED`、`BINARY_FRIENDLY`、`CUSTOM_SERIALIZER_REQUIRED` | 按序列化模型过滤。 |
| `lingonexus.allowed-sandbox-transport-payload-profiles` | list | 空 | `SMALL_PAYLOAD_ONLY`、`STANDARD_PAYLOAD`、`LARGE_PAYLOAD_FRIENDLY` | 按 payload 规模过滤。 |
| `lingonexus.required-sandbox-transport-protocol-capabilities` | list | 空 | `JSON_FRAMED`、`CBOR_CAPABLE`、`STREAMING_CAPABLE`、`CUSTOM_SERIALIZER_CONTRACT` | 要求协议能力。 |
| `lingonexus.required-sandbox-transport-serializer-contract-ids` | list | 空 | 字符串 ID | 要求序列化契约 ID。 |
| `lingonexus.require-engine-cache-capable-sandbox` | boolean | `false` | `true` / `false` | 要求引擎内部缓存支持。 |
| `lingonexus.require-external-process-compatible-sandbox` | boolean | `false` | `true` / `false` | 要求外部进程兼容。 |
| `lingonexus.require-json-safe-external-result` | boolean | `false` | `true` / `false` | 外部结果必须 JSON-safe。 |
| `lingonexus.require-json-safe-external-metadata` | boolean | `false` | `true` / `false` | 外部元数据必须 JSON-safe。 |

示例：

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

### 执行器（异步）

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.executor.core-pool-size` | int | `CPU * 2` | `> 0` | 异步线程池核心线程数。 |
| `lingonexus.executor.max-pool-size` | int | `max(CPU * 4, 50)` | `>= core-pool-size` | 异步线程池最大线程数。 |
| `lingonexus.executor.keep-alive-time-seconds` | long | `60` | `>= 0` | 线程空闲存活时间。 |
| `lingonexus.executor.queue-capacity` | int | `1000` | `> 0` | 队列容量。 |
| `lingonexus.executor.thread-name-prefix` | string | `ScriptExecutor-Async-` | 非空 | 线程名前缀。 |
| `lingonexus.executor.rejection-policy` | string | `CALLER_RUNS` | `CALLER_RUNS`、`ABORT`、`DISCARD`、`DISCARD_OLDEST` | 拒绝策略。 |

### 执行器（隔离线程）

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.isolated-executor.core-pool-size` | int | `32` | `> 0` | `ISOLATED_THREAD` 使用的线程池。 |
| `lingonexus.isolated-executor.max-pool-size` | int | `256` | `>= core-pool-size` | 最大线程数。 |
| `lingonexus.isolated-executor.keep-alive-time-seconds` | long | `60` | `>= 0` | 线程空闲存活时间。 |
| `lingonexus.isolated-executor.queue-capacity` | int | `2000` | `> 0` | 队列容量。 |
| `lingonexus.isolated-executor.thread-name-prefix` | string | `ScriptExecutor-Isolated-` | 非空 | 线程名前缀。 |
| `lingonexus.isolated-executor.rejection-policy` | string | `CALLER_RUNS` | `CALLER_RUNS`、`ABORT`、`DISCARD`、`DISCARD_OLDEST` | 拒绝策略。 |

### 模块与全局变量

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.exclude-script-modules` | list | 空 | 模块名 / 类 / 通配符 | 最高优先级过滤。 |
| `lingonexus.allowed-script-modules` | list | 空 | 模块名 / 类 / 通配符 | 允许列表，空表示不限制。 |
| `lingonexus.global-variables` | map | 空 | 字符串键 | 合并进每次 `ScriptContext`。 |

### 元数据策略

| 配置键 | 类型 | 默认值 | 取值范围 | 说明 |
| --- | --- | --- | --- | --- |
| `lingonexus.metadata.profile` | string | `FULL` | `BASIC`、`TIMING`、`FULL` | 无覆盖时生效。 |
| `lingonexus.metadata.policy` | string | 未设置 | `MINIMAL`、`TIMING`、`DEBUG` | 内置策略。 |
| `lingonexus.metadata.policy-name` | string | 未设置 | 模板名 | 可引用内置策略或自定义模板。 |
| `lingonexus.metadata.categories` | list | 空 | `TIMING`、`THREAD`、`MODULE`、`SECURITY`、`ERROR_DIAGNOSTICS` | 显式指定时覆盖 policy/profile。 |
| `lingonexus.metadata.policy-templates.<name>.parent-policy-name` | string | 未设置 | 内置策略或模板名 | 继承父策略。 |
| `lingonexus.metadata.policy-templates.<name>.categories` | list | 空 | 分类名 | 向模板追加分类。 |

元数据优先级顺序为：`categories` > `policy-name` > `policy` > `profile`。
将 `metadata.categories` 设为空列表可显式关闭详细元数据输出。

### 高级系统参数

| 配置键 | 默认值 | 说明 |
| --- | --- | --- |
| `-Dlingonexus.externalProcess.maxFrameBytes` | `67108864` | 外部进程协议帧上限（字节）。 |

### 完整配置示例

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

### 独立模式 Builder 参考（高级）

Spring Boot 只暴露了核心配置项。独立模式下可使用 `LingoNexusConfig.Builder` 提供更多沙箱筛选与传输约束。

| Builder API | 取值 / 作用 |
| --- | --- |
| `allowedSandboxImplementation(...)` | 仅允许指定沙箱类（全限定名）。 |
| `allowedSandboxLanguage(...)` | 仅允许指定语言 ID（如 `groovy`、`javascript`）。 |
| `allowedSandboxHostAccessMode(...)` | 按宿主访问模型过滤。 |
| `allowedSandboxHostRestrictionMode(...)` | 按限制强度过滤。 |
| `requireSandboxHostRestrictionFlag(...)` | 要求具备指定限制能力。 |
| `allowedSandboxResultTransportMode(...)` | 按 JSON-safe 传输能力过滤。 |
| `allowedSandboxTransportSerializerMode(...)` | 按序列化模型过滤。 |
| `allowedSandboxTransportPayloadProfile(...)` | 按 payload 规模过滤。 |
| `requireSandboxTransportProtocolCapability(...)` | 要求协议级能力。 |
| `requireSandboxTransportSerializerContractId(...)` | 要求自定义序列化契约 ID。 |
| `requireEngineCacheCapableSandbox(true/false)` | 要求引擎内部缓存支持。 |
| `requireExternalProcessCompatibleSandbox(true/false)` | 要求外部进程兼容。 |
| `requireJsonSafeExternalResult(true/false)` | 外部模式下要求结果 JSON-safe。 |
| `requireJsonSafeExternalMetadata(true/false)` | 外部模式下要求元数据 JSON-safe。 |

枚举取值范围：

| 枚举 | 取值 |
| --- | --- |
| `SandboxHostAccessMode` | `JVM_CLASSLOADER`、`POLYGLOT_HOST`、`BYTECODE_COMPILER`、`SCRIPTING_HOST`、`EXPRESSION_ENGINE` |
| `SandboxHostRestrictionMode` | `UNSPECIFIED`、`STRICT`、`MODERATE`、`RELAXED` |
| `SandboxHostRestrictionFlag` | `REFLECTION_BLOCKED`、`FILE_IO_BLOCKED`、`NETWORK_BLOCKED` |
| `SandboxResultTransportMode` | `ANY`、`JSON_SAFE_RESULT`、`JSON_SAFE_RESULT_AND_METADATA` |
| `SandboxTransportSerializerMode` | `JSON_FRAMED`、`BINARY_FRIENDLY`、`CUSTOM_SERIALIZER_REQUIRED` |
| `SandboxTransportPayloadProfile` | `SMALL_PAYLOAD_ONLY`、`STANDARD_PAYLOAD`、`LARGE_PAYLOAD_FRIENDLY` |
| `SandboxTransportProtocolCapability` | `JSON_FRAMED`、`CBOR_CAPABLE`、`STREAMING_CAPABLE`、`CUSTOM_SERIALIZER_CONTRACT` |

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
- 诊断示例：`lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/DiagnosticsExample.java`
- 构建与排障说明：`docs/build-troubleshooting.md`
- 构建验证预设：`scripts/BUILD_PRESETS.md`
- 性能基线：`docs/performance-baseline.md`
- 性能报告索引：`docs/performance-reports/INDEX.md`
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

## 性能快照（2026-03-19）

- 外部进程复用：冷启动 770 ms，重复均值 2.00 ms（executor cache hits 20，misses 1）
- 隔离模式对比（重复均值）：DIRECT 0.40 ms，ISOLATED_THREAD 0.60 ms，EXTERNAL_PROCESS 2.45 ms
- 大上下文对比（重复均值）：DIRECT 0.20 ms，ISOLATED_THREAD 0.50 ms，EXTERNAL_PROCESS 4.00 ms
- direct-path metadata profile 对比（重复均值）：BASIC 0.10 ms，TIMING 0.05 ms，FULL 0.10 ms
- direct-path 上下文来源对比（重复均值）：global-heavy 0.50 ms，request-heavy 0.30 ms
- direct-path 模块使用对比（重复均值）：simple arithmetic 0.05 ms，module-heavy 0.40 ms
- direct-path 失败诊断对比（重复均值）：success-full 0.05 ms，error-diagnostics-only 0.00 ms，failure-full 0.00 ms
- 详细说明与最新报告：`docs/performance-baseline.md`、`docs/performance-reports/INDEX.md`

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
