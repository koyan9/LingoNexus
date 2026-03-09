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

package io.github.koyan9.lingonexus.testcase.springboot;

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Configuration Test
 * Tests application.yml configuration and @Autowired dependency injection
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-03
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Configuration and Dependency Injection Test")
public class SpringBootConfigurationTest {

    @Autowired
    private LingoNexusExecutor lingoNexusExecutor;

    @Autowired
    private LingoNexusConfig lingoNexusConfig;

    @Test
    @DisplayName("Should successfully inject LingoNexusExecutor via @Autowired")
    void shouldInjectLingoNexusExecutor() {
        // Then
        assertThat(lingoNexusExecutor).isNotNull();
        assertThat(lingoNexusExecutor).isInstanceOf(LingoNexusExecutor.class);
    }

    @Test
    @DisplayName("Should successfully inject LingoNexusConfig via @Autowired")
    void shouldInjectLingoNexusConfig() {
        // Then
        assertThat(lingoNexusConfig).isNotNull();
        assertThat(lingoNexusConfig).isInstanceOf(LingoNexusConfig.class);
    }

    @Test
    @DisplayName("Should use default language from configuration")
    void shouldUseDefaultLanguageFromConfig() {
        // Given - application.yml configured with default-language: groovy
        // Then
        assertThat(lingoNexusConfig.getDefaultLanguage()).isEqualTo("groovy");
    }

    @Test
    @DisplayName("Should use cache configuration from YAML")
    void shouldUseCacheConfigFromYaml() {
        // Given - application.yml configured with cache settings
        // Then
        assertThat(lingoNexusConfig.getCacheConfig().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should use sandbox configuration from YAML")
    void shouldUseSandboxConfigFromYaml() {
        // Given - application.yml configured with sandbox settings
        // Then
        assertThat(lingoNexusConfig.getSandboxConfig().isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should execute simple Groovy script")
    void shouldExecuteSimpleGroovyScript() {
        // Given
        String script = "return 'Hello, LingoNexus!'";
        ScriptContext context = ScriptContext.builder().build();

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("Hello, LingoNexus!");
    }

    @Test
    @DisplayName("Should execute script with variables")
    void shouldExecuteScriptWithVariables() {
        // Given
        String script = "return name + ' is ' + age + ' years old'";
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", 25);
        ScriptContext context = ScriptContext.of(variables);

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("Alice is 25 years old");
    }

    @Test
    @DisplayName("Should use built-in Math module")
    void shouldUseBuiltInMathModule() {
        // Given
        String script = "return math.max([10, 20, 30])";
        ScriptContext context = ScriptContext.builder().build();

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should use built-in String module")
    void shouldUseBuiltInStringModule() {
        // Given
        String script = "return str.toUpperCase('hello world')";
        ScriptContext context = ScriptContext.builder().build();

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("HELLO WORLD");
    }

    @Test
    @DisplayName("Should use built-in Date module")
    void shouldUseBuiltInDateModule() {
        // Given
        String script = "return date.now('yyyy-MM-dd').length() == 10";
        ScriptContext context = ScriptContext.builder().build();

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Should use built-in JSON module")
    void shouldUseBuiltInJsonModule() {
        // Given
        String script = "def obj = [name: 'test', value: 123]; return json.stringify(obj)";
        ScriptContext context = ScriptContext.builder().build();

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue().toString()).contains("\"name\"");
        assertThat(result.getValue().toString()).contains("\"test\"");
    }

    @Test
    @DisplayName("Should execute JavaScript script")
    void shouldExecuteJavaScriptScript() {
        // Given
        String script = "x + y";
        Map<String, Object> variables = new HashMap<>();
        variables.put("x", 100);
        variables.put("y", 200);
        ScriptContext context = ScriptContext.of(variables);

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "javascript", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(300);
    }

    @Test
    @DisplayName("Should execute complex business logic script")
    void shouldExecuteComplexBusinessLogicScript() {
        // Given
        String script = """
            def calculateDiscount(price, customerType) {
                if (customerType == 'VIP') {
                    return price * 0.8
                } else if (customerType == 'REGULAR') {
                    return price * 0.9
                } else {
                    return price
                }
            }

            def totalPrice = 0
            items.each { item ->
                totalPrice += calculateDiscount(item.price, customerType)
            }

            return totalPrice
            """;

        Map<String, Object> variables = new HashMap<>();
        variables.put("customerType", "VIP");
        variables.put("items", java.util.List.of(
                java.util.Map.of("name", "Item1", "price", 100),
                java.util.Map.of("name", "Item2", "price", 200),
                java.util.Map.of("name", "Item3", "price", 300)
        ));
        ScriptContext context = ScriptContext.of(variables);

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        // VIP discount 80%: (100 + 200 + 300) * 0.8 = 480
        // Note: Groovy may return BigDecimal, so convert to double for comparison
        double actualValue = ((Number) result.getValue()).doubleValue();
        assertThat(actualValue).isEqualTo(480.0);

        Map<String, Object> variables2 = new HashMap<>();
        variables2.put("customerType", "REGULAR");
        variables2.put("items", java.util.List.of(
                java.util.Map.of("name", "Item1", "price", 100),
                java.util.Map.of("name", "Item2", "price", 200),
                java.util.Map.of("name", "Item3", "price", 300)
        ));
        ScriptContext context2 = ScriptContext.of(variables2);
        ScriptResult result2 = lingoNexusExecutor.execute(script, "groovy", context2);

        assertThat(result2.isSuccess()).isTrue();
        // REGULAR discount 80%: (100 + 200 + 300) * 0.9 = 540.0
        // Note: Groovy may return BigDecimal, so convert to double for comparison
        double actualValue2 = ((Number) result2.getValue()).doubleValue();
        assertThat(actualValue2).isEqualTo(540.0);

    }

    @Test
    @DisplayName("Should verify script execution metadata")
    void shouldVerifyScriptExecutionMetadata() {
        // Given
        String script = "return 1 + 1";
        ScriptContext context = ScriptContext.builder().build();

        // When
        ScriptResult result = lingoNexusExecutor.execute(script, "groovy", context);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMetadata()).isNotNull();
        assertThat(result.getMetadata()).containsKey("scriptEngine");
        assertThat(result.getMetadata().get("scriptEngine")).isEqualTo("groovy");
    }

    @Test
    @DisplayName("Should bind metadata policy templates from application YAML")
    void shouldBindMetadataPolicyTemplatesFromApplicationYaml() {
        assertThat(lingoNexusConfig.getResultMetadataPolicyTemplates()).containsKey("SPRING-TIMING-THREAD");
    }

    @Test
    @DisplayName("Should apply YAML-backed metadata policy template during execution")
    void shouldApplyYamlBackedMetadataPolicyTemplateDuringExecution() {
        ScriptContext context = ScriptContext.builder()
                .put("value", 21)
                .putMetadata(MetadataKeys.RESULT_METADATA_POLICY, "spring-timing-thread")
                .build();

        ScriptResult result = lingoNexusExecutor.execute("return value * 2", "groovy", context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getMetadata().get(ResultMetadataKeys.TOTAL_TIME)).isNotNull();
        assertThat(result.getMetadata().get(ResultMetadataKeys.THREAD_NAME)).isNotNull();
        assertThat(result.getMetadata()).doesNotContainKey(ResultMetadataKeys.MODULES_USED);
    }
}
