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

package io.github.koyan9.lingonexus.testcase.nospring.script.kotlin;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.script.kotlin.KotlinHostSandbox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for KotlinHostSandbox
 */
class KotlinHostSandboxTest {

    private KotlinHostSandbox sandbox;

    @BeforeEach
    void setUp() {
        EngineConfig config = EngineConfig.builder().build();
        sandbox = new KotlinHostSandbox(config);
    }

    @Test
    @DisplayName("Should support Kotlin language identifiers")
    void shouldSupportKotlin() {
        Assertions.assertTrue(sandbox.supports("kotlin"));
        Assertions.assertTrue(sandbox.supports("kts"));
        Assertions.assertTrue(sandbox.supports("Kotlin"));
        Assertions.assertFalse(sandbox.supports("groovy"));
        Assertions.assertFalse(sandbox.supports(""));
    }

    @Test
    @DisplayName("Should compile and execute script")
    void shouldCompileAndExecute() {
        // Kotlin JSR-223 requires variables to be defined at compile time
        // Use a self-contained script instead
        String script = """
                fun multiply(x: Int): Int = x * 2
                multiply(5)
                """;

        CompiledScript compiled = sandbox.compile(script, ScriptContext.of(null));
        Object result1 = compiled.execute(null);
        assertEquals(10, result1);

        // Test with different script
        String script2 = """
                fun multiply(x: Int): Int = x * 2
                multiply(10)
                """;
        CompiledScript compiled2 = sandbox.compile(script2, ScriptContext.of(null));
        Object result2 = compiled2.execute(null);
        assertEquals(20, result2);
    }

    @Test
    @DisplayName("Should return correct language from compiled script")
    void shouldReturnCorrectLanguage() {
        CompiledScript compiled = sandbox.compile("1 + 1", ScriptContext.of(null));
        assertEquals("kotlin", compiled.getLanguage());
    }

    @Test
    @DisplayName("Should compile and execute target Kotlin script")
    void shouldCompileAndExecuteTargetScript() {
        String script = "val x = 10\nval y = 20\nx + y";

        CompiledScript compiled = sandbox.compile(script, ScriptContext.of(null));
        assertNotNull(compiled);
        assertEquals("kotlin", compiled.getLanguage());

        Object result = compiled.execute(null);

        assertNotNull(result);
        assertEquals(30, result);
    }

    @Test
    @DisplayName("Should execute with different variables")
    void shouldExecuteWithDifferentVariables() {
        String script = "a + b";
        ScriptContext context = ScriptContext.builder()
                .put("a", 10)
                .put("b", 20)
                .build();

        CompiledScript compiledScript = sandbox.compile(script, context);

        Object result = compiledScript.execute(context);

        assertNotNull(result);
        assertEquals(30, result);

        context.setVariable("a", Integer.valueOf(20));
        context.setVariable("b", Integer.valueOf(30));

        Object result2 = compiledScript.execute(context);

        assertNotNull(result2);
        assertEquals(50, result2);
    }

}
