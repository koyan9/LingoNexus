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
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Basic Integration Test
 * Tests basic functionality of LingoNexus with Spring Boot auto-configuration
 *
 * @author lingonexus Team
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Basic Integration Test")
public class SpringBootBasicIntegrationTest {

    @Autowired
    private LingoNexusExecutor facade;

    @Test
    @DisplayName("Should execute simple script via Spring Boot auto-configuration")
    void shouldExecuteSimpleScriptViaSpringBoot() {
        // Given
        String script = "return 1 + 2";

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should execute script with variables via Spring Boot auto-configuration")
    void shouldExecuteScriptWithVariablesViaSpringBoot() {
        // Given
        String script = "return x * y";
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 6);
        vars.put("y", 7);

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should use Math module via Spring Boot auto-configuration")
    void shouldUseMathModuleViaSpringBoot() {
        // Given
        String script = "return math.max([10, 20, 30, 5])";

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        ScriptResult result2 = facade.execute("return math.max(10, 21)", "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(30.0);
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result2.getValue()).isEqualTo(21.0);
    }

    @Test
    @DisplayName("Should use String module via Spring Boot auto-configuration")
    void shouldUseStringModuleViaSpringBoot() {
        // Given
        String script = "return str.reverse('hello')";

        // When
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo("olleh");
    }

    @Test
    @DisplayName("Should execute JavaScript via Spring Boot auto-configuration")
    void shouldExecuteJavaScriptViaSpringBoot() {
        // Given
        String script = "a + b";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 100);
        vars.put("b", 200);

        // When
        ScriptResult result = facade.execute(script, "javascript", ScriptContext.of(vars));

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(300);
    }

    @Test
    @DisplayName("Should verify Spring Boot auto-configuration components")
    void shouldVerifySpringBootAutoConfiguration() {
        // Then
        assertThat(facade).isNotNull();
    }
}
