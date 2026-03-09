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

import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Business Modules Integration Test
 * Tests business module functionality with Spring Boot auto-configuration
 *
 * @author lingonexus Team
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Business Modules Integration Test")
public class SpringBootModulesIntegrationTest {

    @Autowired
    private LingoNexusExecutor facade;

    @Test
    @DisplayName("Should use Validator module via Spring Boot auto-configuration")
    void shouldUseValidatorModuleViaSpringBoot() {
        // Given
        String script = "return validator.isEmail('admin@lingonexus.io')";

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Should use Codec module via Spring Boot auto-configuration")
    void shouldUseCodecModuleViaSpringBoot() {
        // Given
        String script = """
                    def original = 'LingoNexus'
                    def encoded = codec.base64Encode(original)
                    def decoded = codec.base64Decode(encoded)
                    return decoded == original
                """;

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Should use Date module via Spring Boot auto-configuration")
    void shouldUseDateModuleViaSpringBoot() {
        // Given
        String script = "return date.now('yyyy-MM-dd') != null";

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(true);
    }

    @Test
    @DisplayName("Should use JSON module via Spring Boot auto-configuration")
    void shouldUseJsonModuleViaSpringBoot() {
        // Given
        String script = """
                    def jsonStr = '{"framework":"LingoNexus","version":"1.0.0"}'
                    def obj = json.parse(jsonStr)
                    return obj.framework
                """;

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("LingoNexus");
    }

    @Test
    @DisplayName("Should combine multiple modules via Spring Boot auto-configuration")
    void shouldCombineMultipleModulesViaSpringBoot() {
        // Given
        String script = """
                    def email = 'test@example.com'
                    def isValid = validator.isEmail(email)
                    def upper = str.toUpperCase(email)
                    def encoded = codec.base64Encode(email)
                    return [valid: isValid, upper: upper, encoded: encoded]
                """;

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("Should execute complex business logic via Spring Boot auto-configuration")
    void shouldExecuteComplexBusinessLogicViaSpringBoot() {
        // Given
        String script = """
                    def numbers = [1, 2, 3, 4, 5]
                    def sum = math.sum(numbers)
                    def avg = math.avg(numbers)
                    def result = "Sum: " + sum + ", Avg: " + avg
                    return str.toUpperCase(result)
                """;

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("SUM: 15.0, AVG: 3.0");
    }
}
