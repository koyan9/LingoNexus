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

package io.github.koyan9.lingonexus.testcase.nospring.script.groovy;

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.script.groovy.GroovySandbox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GroovySandbox
 */
class GroovySandboxTest {

    private GroovySandbox sandbox;

    @BeforeEach
    void setUp() {
        EngineConfig config = EngineConfig.builder().build();
        sandbox = new GroovySandbox(config);
    }

    @Test
    @DisplayName("Should support Groovy language identifiers")
    void shouldSupportGroovy() {
        assertTrue(sandbox.supports("groovy"));
        assertTrue(sandbox.supports("Groovy"));
        assertTrue(sandbox.supports("GROOVY"));
        assertFalse(sandbox.supports("java"));
        assertFalse(sandbox.supports(null));
        assertFalse(sandbox.supports(""));
    }

    @Test
    @DisplayName("Should compile and execute script")
    void shouldCompileAndExecute() {
        ScriptContext context1 = ScriptContext.builder().put("x", 5).build();
        CompiledScript compiled = sandbox.compile("x * 2", context1);

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
        assertEquals("groovy", compiled.getLanguage());
    }
}

