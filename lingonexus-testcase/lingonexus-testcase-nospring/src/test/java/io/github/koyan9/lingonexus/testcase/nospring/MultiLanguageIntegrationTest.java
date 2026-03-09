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
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 多语言集成测试
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-03
 */
@DisplayName("Multi-Language Integration Tests")
class MultiLanguageIntegrationTest {

    private LingoNexusExecutor facade;

    @BeforeEach
    void setUp() {
        // 创建完整的引擎配置
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder().build())
                .build();

        // 使用新的 API：通过类自动构造 Sandbox
        facade = LingoNexusBuilder.loadInstance(config);
    }

    @Test
    @DisplayName("Should execute Groovy script")
    void shouldExecuteGroovyScript() {
        // Given
        String script = "def sum = a + b; return sum * 2";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 5);
        vars.put("b", 10);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should execute JavaScript script")
    void shouldExecuteJavaScriptScript() {
        // Given
        String script = "a + b";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 5);
        vars.put("b", 10);

        // When
        ScriptResult result = facade.execute(script, "javascript", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should switch between languages")
    void shouldSwitchBetweenLanguages() {
        // Given
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 10);

        // When - Execute Groovy
        ScriptResult groovyResult = facade.execute("x * 2", "groovy", ScriptContext.of(vars));

        // When - Execute JavaScript
        ScriptResult jsResult = facade.execute("x * 3", "javascript", ScriptContext.of(vars));

        // Then
        assertThat(groovyResult.getValue()).isEqualTo(20);
        assertThat(jsResult.getValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should get supported languages")
    void shouldGetSupportedLanguages() {
        // When
        Object languages = facade.getSupportedLanguages();

        // Then
        assertThat(languages).isNotNull();
        assertThat(languages.toString()).contains("groovy");
        assertThat(languages.toString()).contains("javascript");
    }
}
