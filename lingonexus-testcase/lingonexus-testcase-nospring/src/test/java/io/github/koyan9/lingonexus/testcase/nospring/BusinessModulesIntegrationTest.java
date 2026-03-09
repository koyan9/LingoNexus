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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务模块集成测试
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-03
 */
@DisplayName("Business Modules Integration Tests")
class BusinessModulesIntegrationTest {

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
    }

    @Test
    @DisplayName("Should validate email using validator module")
    void shouldValidateEmailUsingValidatorModule() {
        // Given
        String script = "validator.isEmail(email)";
        Map<String, Object> vars = new HashMap<>();
        vars.put("email", "test@example.com");

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Should format number using formatter module")
    void shouldFormatNumberUsingFormatterModule() {
        // Given
        String script = "formatter.number(value, '#.00')";
        Map<String, Object> vars = new HashMap<>();
        vars.put("value", 1234.5678);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
        // 补充断言：验证格式化结果符合预期（1234.57）
        assertThat(result.getValue()).isEqualTo("1234.57");
    }

    @Test
    @DisplayName("Should encode and decode using codec module")
    void shouldEncodeAndDecodeUsingCodecModule() {
        // Given
        String script = """
                def encoded = codec.base64Encode(text)
                def decoded = codec.base64Decode(encoded)
                return decoded
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("text", "Hello World");

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("Should format date using date module")
    void shouldFormatDateUsingDateModule() {
        // Given
        String script = "date.now('yyyy-MM-dd')";
        Map<String, Object> vars = new HashMap<>();

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().toString()).matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @Test
    @DisplayName("Should parse and stringify JSON using json module")
    void shouldParseAndStringifyJsonUsingJsonModule() {
        // Given
        String script = """
                def obj = json.parse(jsonStr)
                return json.stringify(obj)
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("jsonStr", "{\"name\":\"test\",\"value\":123}");

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().toString()).contains("name");
        assertThat(result.getValue().toString()).contains("test");
    }

    @Test
    @DisplayName("Should parse and stringify JSON using custom json module")
    void shouldParseAndStringifyJsonUsingCustomJsonModule() {
        // Given
        String script = """
                def obj = customJson.parse123(jsonStr)
                return obj.name
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("jsonStr", "{\"name\":\"test\",\"value\":123}");

        facade.registerModule(new ScriptModule() {

            private final ObjectMapper objectMapper = new ObjectMapper();

            @Override
            public String getName() {
                return "customJson";
            }

            @Override
            public Map<String, Object> getFunctions() {
                HashMap<String, Object> map = new HashMap<>();
                map.put("parse123", (Function<String, Object>) this::parse123);
                return map;
            }

            private Object parse123(String jsonStr) {
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
                return "parse123".equals(functionName);
            }
        });

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue().toString()).isEqualTo("test");
    }

    @Test
    @DisplayName("Should combine multiple modules in complex scenario")
    void shouldCombineMultipleModulesInComplexScenario() {
        // Given
        String script = """
                // Validate email
                if (!validator.isEmail(email)) {
                    return "Invalid email"
                }
                
                // Format date
                def today = date.now('yyyy-MM-dd')
                
                // Create JSON
                def data = [email: email, date: today, amount: amount]
                def jsonStr = json.stringify(data)
                
                // Encode
                return codec.base64Encode(jsonStr)
                """;
        Map<String, Object> vars = new HashMap<>();
        vars.put("email", "user@example.com");
        vars.put("amount", 100.50);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.getValue()).isNotNull();
        assertThat(result.getValue()).isInstanceOf(String.class);
    }
}
