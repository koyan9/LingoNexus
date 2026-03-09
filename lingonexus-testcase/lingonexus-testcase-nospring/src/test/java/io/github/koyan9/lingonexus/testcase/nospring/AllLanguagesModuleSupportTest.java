/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.koyan9.lingonexus.testcase.nospring;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 所有脚本语言的模块支持测试
 * <p>测试所有支持的脚本语言（Groovy、JavaScript、Java、Kotlin）对自定义模块和内置模块的支持</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-26
 */
@DisplayName("All Languages Module Support Tests")
public class AllLanguagesModuleSupportTest {

    private LingoNexusExecutor facade;

    @BeforeEach
    void setUp() {
        CacheConfig cacheConfig = CacheConfig.builder()
                .enabled(true)
                .build();
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .build();
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(cacheConfig)
                .sandboxConfig(sandboxConfig)
                .build();

        facade = LingoNexusBuilder.loadInstance(config);

        facade.registerModule(createCustomJsonModule());
    }

    @AfterAll
    static void tearDown() {
        LingoNexusBuilder.resetInstance();
    }

    // ==================== Groovy 测试 ====================

    @Test
    @DisplayName("Groovy - Should use built-in math module")
    void groovy_shouldUseBuiltInMathModule() {
        // Given
        String script = "math.max(a, b)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 20);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Groovy - Should use built-in str module")
    void groovy_shouldUseBuiltInStrModule() {
        // Given
        String script = "str.toUpperCase(text)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("text", "hello");

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Groovy - Should use custom module")
    void groovy_shouldUseCustomModule() {

        String script = """
                def obj = customJson.parse(jsonStr)
                return obj.name
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("jsonStr", "{\"name\":\"test\",\"value\":123}");

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("test");
    }

    // ==================== JavaScript 测试 ====================

    @Test
    @DisplayName("JavaScript - Should use built-in math module")
    void javascript_shouldUseBuiltInMathModule() {
        // Given
        String script = "math.max(a, b)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 20);

        // When
        ScriptResult result = facade.execute(script, "javascript", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(20);
    }

    @Test
    @DisplayName("JavaScript - Should use built-in str module")
    void javascript_shouldUseBuiltInStrModule() {
        // Given
        String script = "str.toUpperCase(text)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("text", "hello");

        // When
        ScriptResult result = facade.execute(script, "javascript", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("JavaScript - Should use custom module")
    void javascript_shouldUseCustomModule() {

        String script = """
                var obj = customJson.parse(jsonStr);
                obj.name;
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("jsonStr", "{\"name\":\"test\",\"value\":123}");

        // When
        ScriptResult result = facade.execute(script, "javascript", ScriptContext.of(vars));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("test");
    }

    // ==================== Java 测试 ====================

    @Test
    @DisplayName("Java - Should use built-in math module")
    void java_shouldUseBuiltInMathModule() {
        // Given
        String script = "math.max(a, b)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 20);

        // When
        ScriptResult result = facade.execute(script, "java", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Java - Should use built-in str module")
    void java_shouldUseBuiltInStrModule() {
        // Given
        String script = "str.toUpperCase(text)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("text", "hello");

        // When
        ScriptResult result = facade.execute(script, "java", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Java - Should use custom module")
    void java_shouldUseCustomModule() {
        // Given
        String script = "customJson.parse(jsonStr)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("jsonStr", "{\"name\":\"test\",\"value\":123}");

        // When
        ScriptResult result = facade.execute(script, "java", ScriptContext.of(vars));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue()).isInstanceOf(java.util.Map.class);
    }

    // ==================== Kotlin 测试 ====================

    @Test
    @DisplayName("Kotlin - Should use built-in math module")
    void kotlin_shouldUseBuiltInMathModule() {
        // Given
        String script = "math.max(a, b)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10.0);
        vars.put("b", 20.0);

        // When
        ScriptResult result = facade.execute(script, "kotlin", ScriptContext.of(vars));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Kotlin - Should use built-in str module")
    void kotlin_shouldUseBuiltInStrModule() {
        // Given
        String script = "str.toUpperCase(text)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("text", "hello");

        // When
        ScriptResult result = facade.execute(script, "kotlin", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("HELLO");
    }

    @Test
    @DisplayName("Kotlin - Should use custom module")
    void kotlin_shouldUseCustomModule() {

        String script = """
                val obj = customJson.parse(jsonStr) as Map<*, *>
                obj["name"]
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("jsonStr", "{\"name\":\"test\",\"value\":123}");

        // When
        ScriptResult result = facade.execute(script, "kotlin", ScriptContext.of(vars));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("test");
    }

    // ==================== 综合测试 ====================

    @Test
    @DisplayName("All Languages - Should use validator module")
    void allLanguages_shouldUseValidatorModule() {
        // Test Groovy
        ScriptResult groovyResult = facade.execute(
                "validator.isEmail(email)",
                "groovy",
                ScriptContext.of(Map.of("email", "test@example.com"))
        );
        assertThat(groovyResult.getValue()).isEqualTo(true);

        // Test JavaScript
        ScriptResult jsResult = facade.execute(
                "validator.isEmail(email)",
                "javascript",
                ScriptContext.of(Map.of("email", "test@example.com"))
        );
        assertThat(jsResult.getValue()).isEqualTo(true);

        // Test Java
        ScriptResult javaResult = facade.execute(
                "validator.isEmail(email)",
                "java",
                ScriptContext.of(Map.of("email", "test@example.com"))
        );
        assertThat(javaResult.getValue()).isEqualTo(true);

        ScriptResult javaExprResult = facade.execute(
                "validator.isEmail(email)",
                "javaexpr",
                ScriptContext.of(Map.of("email", "test@example.com"))
        );
        assertThat(javaExprResult.getValue()).isEqualTo(true);

        // Test Kotlin
        ScriptResult kotlinResult = facade.execute(
                "validator.isEmail(email)",
                "kotlin",
                ScriptContext.of(Map.of("email", "test@example.com"))
        );
        assertThat(kotlinResult.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("All Languages - Should use json module")
    void allLanguages_shouldUseJsonModule() {
        String jsonStr = "{\"name\":\"test\",\"value\":123}";

        // Test Groovy
        ScriptResult groovyResult = facade.execute(
                "def obj = json.parse(jsonStr); return obj.name",
                "groovy",
                ScriptContext.of(Map.of("jsonStr", jsonStr))
        );
        assertThat(groovyResult.getValue()).isEqualTo("test");

        // Test JavaScript
        ScriptResult jsResult = facade.execute(
                "var obj = json.parse(jsonStr); obj.name;",
                "javascript",
                ScriptContext.of(Map.of("jsonStr", jsonStr))
        );
        assertThat(jsResult.getValue()).isEqualTo("test");

        // Test Java (只验证解析成功)
        ScriptResult javaResult = facade.execute(
                "json.parse(jsonStr)",
                "java",
                ScriptContext.of(Map.of("jsonStr", jsonStr))
        );
        assertThat(javaResult.getValue()).isNotNull();

        ScriptResult javaExprResult = facade.execute(
                "json.parse(jsonStr)",
                "javaexpr",
                ScriptContext.of(Map.of("jsonStr", jsonStr))
        );
        assertThat(javaExprResult.getValue()).isNotNull();

        // Test Kotlin
        ScriptResult kotlinResult = facade.execute(
                "val obj = json.parse(jsonStr) as Map<*, *>; obj[\"name\"]",
                "kotlin",
                ScriptContext.of(Map.of("jsonStr", jsonStr))
        );
        assertThat(kotlinResult.getValue()).isEqualTo("test");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建自定义 JSON 模块
     */
    private ScriptModule createCustomJsonModule() {
        return new CustomJsonModule();
    }

    /**
     * 自定义 JSON 模块实现
     */
    public static class CustomJsonModule implements ScriptModule {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public String getName() {
            return "customJson";
        }

        @Override
        public Map<String, Object> getFunctions() {
            HashMap<String, Object> map = new HashMap<>();
            map.put("parse", (Function<String, Object>) this::parse);
            return map;
        }

        public Object parse(String jsonStr) {
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                return null;
            }
            try {
                return objectMapper.readValue(jsonStr, Object.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to parse JSON string", e);
            }
        }

        @Override
        public boolean hasFunction(String functionName) {
            return "parse".equals(functionName);
        }
    }
}
