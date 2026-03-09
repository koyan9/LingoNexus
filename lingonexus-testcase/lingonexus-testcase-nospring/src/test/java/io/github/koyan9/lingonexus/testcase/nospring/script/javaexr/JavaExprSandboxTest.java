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

package io.github.koyan9.lingonexus.testcase.nospring.script.javaexr;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.script.javaexpr.JavaExprSandbox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for JavaExprSandbox
 */
class JavaExprSandboxTest {

    private JavaExprSandbox sandbox;

    @BeforeEach
    void setUp() {
        EngineConfig config = EngineConfig.builder().build();
        sandbox = new JavaExprSandbox(config);
    }

    @Test
    @DisplayName("Should support Java expression language identifiers")
    void shouldSupportJava() {
        Assertions.assertTrue(sandbox.supports("javaexpr"));
        Assertions.assertTrue(sandbox.supports("jexpr"));
        Assertions.assertFalse(sandbox.supports("java")); // "java" now refers to Janino
        Assertions.assertFalse(sandbox.supports("groovy"));
        Assertions.assertFalse(sandbox.supports(null));
        Assertions.assertFalse(sandbox.supports(""));
    }

    @Test
    @DisplayName("Should compile and execute expression")
    void shouldCompileAndExecute() {
        ScriptContext scriptContext = ScriptContext.builder().put("x", 5).build();
        CompiledScript compiled = sandbox.compile("x * 2", scriptContext);

        ScriptContext context1 = ScriptContext.builder().put("x", 5).build();
        Object result1 = compiled.execute(context1);
        assertEquals(10, result1);

        ScriptContext context2 = ScriptContext.builder().put("x", 10).build();
        Object result2 = compiled.execute(context2);
        assertEquals(20, result2);
    }

    @Test
    @DisplayName("Should return correct language from compiled script")
    void shouldReturnCorrectLanguage() {
        CompiledScript compiled = sandbox.compile("1 + 1", null);
        assertEquals("javaexpr", compiled.getLanguage());
    }
}
