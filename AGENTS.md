# lingonexus - AI Agent Guidelines

## 构建与测试命令

### Maven 构建命令
```bash
# 编译整个项目
mvn clean compile

# 打包整个项目
mvn clean package

# 安装到本地仓库
mvn clean install

# 跳过测试打包
mvn clean package -DskipTests

# 清理构建产物
mvn clean
```

### 测试命令
```bash
# 运行所有测试
mvn test

# 运行单个测试类
mvn test -Dtest=ClassName

# 运行单个测试方法
mvn test -Dtest=ClassName#methodName

# 运行特定模块的测试
mvn test -pl lingonexus-core

# 运行测试并生成覆盖率报告
mvn test jacoco:report
```

### 单模块操作
```bash
# 构建特定模块
mvn clean install -pl lingonexus-api

# 构建模块及其依赖
mvn clean install -pl lingonexus-core -am

# 运行特定模块测试
mvn test -pl lingonexus-script-groovy
```

## 代码风格指南

### 文件编码
- 所有文件使用 UTF-8 编码
- 行尾使用 LF (Unix 风格)
- 文件末尾保留一个空行

### Java 代码风格

#### 命名约定
- 类名：PascalCase (如 `ScriptEngineFacade`)
- 方法名：camelCase (如 `executeScript`)
- 变量名：camelCase (如 `scriptContext`)
- 常量：UPPER_SNAKE_CASE (如 `MAX_SCRIPT_SIZE`)
- 包名：全小写，使用点分隔 (如 `io.lingonexus.api`)
- 接口：PascalCase，可以使用 I 前缀或描述性名称 (如 `ScriptSandbox`)

#### 导入顺序
```java
// 1. JDK 标准库
import java.util.*;
import java.util.concurrent.*;

// 2. 第三方库
import com.fasterxml.jackson.databind.*;
import com.github.benmanes.caffeine.cache.*;

// 3. 项目内部包
import io.github.koyan9.lingonexus.api.*;
import io.github.koyan9.lingonexus.core.*;

// 4. 静态导入（单独放在最后）
import static org.junit.Assert.*;
```

#### 方法长度和复杂度
- 单个方法不超过 50 行
- 圈复杂度不超过 10
- 每个方法只做一件事

#### 注释规范
```java
/**
 * 脚本引擎外观接口，提供统一的脚本执行入口
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface ScriptEngineFacade {

    /**
     * 执行脚本
     *
     * @param script 脚本内容，不能为 null
     * @param language 脚本语言，支持 groovy/javascript
     * @param context 脚本上下文，包含变量和参数
     * @return 执行结果，包含状态和数据
     * @throws ScriptException 脚本执行异常
     * @throws ScriptSecurityException 脚本安全异常
     */
    ScriptResult execute(String script, String language, ScriptContext context);
}
```

### 异常处理规范
```java
// 检查参数，使用 Objects.requireNonNull
public void execute(String script) {
    Objects.requireNonNull(script, "script 不能为 null");
}

// 使用 try-with-resources 管理资源
try (ScriptSandbox sandbox = createSandbox()) {
    return sandbox.execute(script, context);
}

// 具体异常优先于通用异常
throw new ScriptSecurityException("禁止访问类: " + className);

// 不要吞掉异常
try {
    // 代码
} catch (Exception e) {
    log.error("执行脚本失败", e);
    throw new ScriptException(e);  // 重新抛出或包装
}
```

### 日志规范
```java
// 使用 SLF4J
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

private static final Logger log = LoggerFactory.getLogger(ScriptEngineFacadeImpl.class);

// 日志级别使用
log.debug("调试信息: script={}", script);      // 调试信息
log.info("脚本执行成功: id={}", scriptId);      // 重要业务事件
log.warn("警告: 脚本大小超过建议值");            // 警告但不影响运行
log.error("执行失败: error={}", e.getMessage(), e);  // 错误，记录异常堆栈
```

### 设计模式与架构原则

#### 依赖注入
- 优先使用构造函数注入
- 依赖接口而非实现类
```java
// 好的做法
public class ScriptEngineFacadeImpl implements ScriptEngineFacade {
    private final List<ScriptSandbox> sandboxes;

    public ScriptEngineFacadeImpl(List<ScriptSandbox> sandboxes) {
        this.sandboxes = Objects.requireNonNull(sandboxes);
    }
}
```

#### Builder 模式
- 复杂对象使用 Builder 模式
```java
ScriptContext context = ScriptContext.builder()
    .put("value", "test")
    .put("index", 0)
    .build();
```

### 安全编程规范
- 所有外部输入必须验证
- 使用白名单而非黑名单进行类访问控制
- 限制脚本大小、执行时间、循环次数
- 禁止文件 I/O、网络访问、反射操作
```java
// 验证脚本大小
if (script.length() > sandboxConfig.getMaxScriptSize()) {
    throw new ScriptException("脚本大小超过限制");
}

// 白名单检查
if (!whitelist.contains(className)) {
    throw new ScriptSecurityException("禁止访问类: " + className);
}
```

### 测试规范
```java
// 使用 JUnit 5
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class ScriptEngineFacadeTest {

    @Test
    @DisplayName("应该成功执行简单的 Groovy 脚本")
    void shouldExecuteSimpleGroovyScript() {
        // Given
        String script = "return value.length() > 5";
        ScriptContext context = ScriptContext.builder()
            .put("value", "lingonexus")
            .build();

        // When
        ScriptResult result = facade.execute(script, "groovy", context);

        // Then
        assertTrue(result.isSuccess());
        assertEquals(true, result.getValue());
    }
}
```

### 模块职责划分
- **lingonexus-api**: 纯接口定义，零依赖
- **lingonexus-core**: 核心业务逻辑实现
- **lingonexus-script-groovy**: Groovy 语言支持
- **lingonexus-script-javascript**: JavaScript 语言支持
- **lingonexus-utils**: 工具函数实现
- **lingonexus-modules**: 可扩展功能模块
- **lingonexus-spring**: Spring 框架集成
- **lingonexus-spring-boot**: Spring Boot 自动配置

### 性能优化原则
- 使用 Caffeine 缓存编译后的脚本
- 异步执行耗时操作
- 对象复用，避免频繁创建
- 使用 StringBuilder 拼接字符串

### 版本管理
- Java 版本：1.8
- 项目版本：1.0.0-SNAPSHOT
- 依赖版本统一在父 POM 的 dependencyManagement 中管理

### Git 提交规范
```
feat: 添加 Groovy 脚本执行功能
fix: 修复脚本缓存失效问题
docs: 更新 API 文档
refactor: 重构沙箱管理器
test: 添加安全策略测试
chore: 升级依赖版本
```

### 常用 IDE 配置建议
- 启用自动保存
- 配置代码格式化模板（Google Java Style）
- 启用 Checkstyle 插件（如果添加 checkstyle.xml）
- 配置 Maven 自动导入依赖
