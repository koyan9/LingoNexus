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

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.ExecutorConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 基础集成测试 - 验证核心功能
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-03
 */
@DisplayName("Basic Integration Tests")
class BasicIntegrationTest {

    private LingoNexusExecutor facade;

    @BeforeEach
    void setUp() {
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder().build())
                .executorConfig(ExecutorConfig.builder().build())
                .build();

        facade = LingoNexusBuilder.loadInstance(config);
    }

    @Test
    @DisplayName("Should execute simple arithmetic expression")
    void shouldExecuteSimpleArithmeticExpression() {
        // Given
        String script = "1 + 2 * 3";
        Map<String, Object> vars = new HashMap<>();

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(7);
    }

    @Test
    @DisplayName("Should execute script with variables")
    void shouldExecuteScriptWithVariables() {
        // Given
        String script = "a + b";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 10);
        vars.put("b", 20);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should use math module")
    void shouldUseMathModule() {
        // Given
        String script = "math.add(0.1, 0.2)";
        Map<String, Object> vars = new HashMap<>();

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
    }

    @Test
    @DisplayName("Should use string module")
    void shouldUseStringModule() {
        // Given
        String script = "str.trim(text)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("text", "  hello  ");

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("hello");
    }

    @Test
    @DisplayName("Should execute complex business logic")
    void shouldExecuteComplexBusinessLogic() {
        // Given
        String script = """
                def price = basePrice * quantity
                def discount = price > 1000 ? 0.1 : 0
                def finalPrice = price * (1 - discount)
                return finalPrice
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("basePrice", 299.99);
        vars.put("quantity", 5);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
        assertThat(((Number) result.getValue()).doubleValue()).isGreaterThan(1000);
    }


    @Test
    @DisplayName("Should fail fast for unsupported language")
    void shouldFailFastForUnsupportedLanguage() {
        // Given
        String script = "return 1";

        // When
        ScriptResult result = facade.execute(script, "unsupported-language", ScriptContext.of(new HashMap<>()));

        // Then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getErrorMessage()).contains("Unsupported language");
        assertThat(result.getMetadata()).isNull();
    }

    @Test
    @DisplayName("Should get supported languages")
    void shouldGetSupportedLanguages() {
        // When
        Object languages = facade.getSupportedLanguages();

        // Then
        assertThat(languages).isNotNull();
    }
}
