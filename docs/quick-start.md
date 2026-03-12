# LingoNexus 快速开始

本指南基于当前代码实现编写，默认以 Java 17 和 Maven 为前提。

> 文档索引：`docs/INDEX.md`  
> 架构基线：`docs/architecture.md`  
> 诊断说明：`docs/diagnostics.md`


## 1. 先了解两种推荐接入方式

当前推荐两种接入方式：

1. **Spring Boot 集成**：适合业务系统直接注入执行器 Bean。
2. **独立模式**：适合无 Spring 环境、工具程序或测试代码。

> 当前对外统一入口是 `LingoNexusExecutor`，不要再使用旧文档里的 `ScriptEngineFacade`。

---

## 2. Spring Boot 5 分钟接入

### 2.1 添加依赖

```xml
<dependency>
    <groupId>io.github.koyan9</groupId>
    <artifactId>lingonexus-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### 2.2 添加基础配置

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

### 2.3 注入执行器并运行脚本

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
                .put("userId", request.getUserId())
                .build();

        return lingoNexusExecutor.execute(
                request.getScript(),
                request.getLanguage(),
                context
        );
    }
}
```

### 2.4 一个最小脚本示例

```groovy
return validator.isEmail(value)
```

如果 `request.getLanguage()` 为 `groovy`，上面的脚本会调用内置 `validator` 模块完成校验。

---

## 3. 独立模式 5 分钟接入

### 3.1 添加依赖

至少需要：

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

如果你还需要 JavaScript、Kotlin 或 Java 相关语言能力，再按需添加对应 `lingonexus-script-*` 模块。

### 3.2 构建引擎

```java
LingoNexusConfig config = LingoNexusConfig.builder()
        .defaultLanguage(ScriptLanguage.GROOVY)
        .sandboxConfig(SandboxConfig.builder()
                .enabled(true)
                .timeoutMs(5000)
                .build())
        .build();

LingoNexusExecutor engine = LingoNexusBuilder.createNewInstance(config);
```

### 3.3 执行脚本

```java
ScriptContext context = ScriptContext.builder()
        .put("price", 299.99)
        .put("quantity", 5)
        .build();

ScriptResult result = engine.execute(
        "math.multiply(price, quantity)",
        "groovy",
        context
);

System.out.println(result.getValue());
System.out.println(result.getExecutionTime());
```

---

## 4. 切换隔离模式

当前支持四种隔离模式：

| 模式 | 说明 |
| --- | --- |
| `AUTO` | 默认模式；开启超时时通常走隔离线程执行 |
| `DIRECT` | 当前线程直接执行，开销最低 |
| `ISOLATED_THREAD` | 使用隔离线程池执行，便于超时控制 |
| `EXTERNAL_PROCESS` | 使用持久化外部 JVM worker 池执行 |

### 4.1 Spring Boot 中切换

```yaml
lingonexus:
  sandbox:
    isolation-mode: EXTERNAL_PROCESS
    timeout-ms: 5000
    external-process-pool-size: 1
    external-process-startup-retries: 2
    external-process-prewarm-count: 1
    external-process-idle-ttl-ms: 300000
    external-process-borrow-timeout-ms: 5000
    # borrow-timeout-ms: 0 means wait indefinitely
```

### 4.2 代码中切换

```java
LingoNexusConfig config = LingoNexusConfig.builder()
        .sandboxConfig(SandboxConfig.builder()
                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                .timeoutMs(5000)
                .build())
        .build();
```

> 当前 `EXTERNAL_PROCESS` 更适合 JSON-safe 的变量、metadata 和结果值。

---

## 5. 获取统计和诊断信息

```java
EngineStatistics statistics = engine.getStatistics();
EngineDiagnostics diagnostics = engine.getDiagnostics();

System.out.println(statistics.getTotalExecutions());
System.out.println(diagnostics.getCacheSize());
System.out.println(diagnostics.getIsolationMode());
```

如果当前使用外部进程模式，还可以通过 `diagnostics.getExternalProcessStatistics()` 查看 worker 池状态。

---

## 6. 常见迁移差异

如果你正在参考仓库中的旧文档或旧示例，请注意以下差异：

- 当前统一入口是 `LingoNexusExecutor`，不是 `ScriptEngineFacade`
- 当前 JavaScript 实现是 `GraalJSSandbox`，不是旧版 Rhino 表述
- Spring Boot 配置键是 `timeout-ms`，不是旧示例中的 `timeout`
- 缓存配置键是 `expire-after-write-ms` / `expire-after-access-ms`
- 独立模式建议使用 `LingoNexusBuilder.createNewInstance(...)` 或 `loadInstance(...)`
- 当前真实模块是 `lingonexus-spring-boot-starter`，不要继续按旧规划写 `lingonexus-spring`

---

## 7. 下一步读什么

- 当前架构总览：`docs/architecture.md`
- 诊断说明：`docs/diagnostics.md`
- 性能基线：`docs/performance-baseline.md`
- 示例代码：`lingonexus-examples/src/main/java/io/github/koyan9/lingonexus/examples/`
