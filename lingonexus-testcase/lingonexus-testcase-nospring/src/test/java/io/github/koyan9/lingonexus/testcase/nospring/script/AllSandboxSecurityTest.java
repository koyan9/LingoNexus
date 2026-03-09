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

package io.github.koyan9.lingonexus.testcase.nospring.script;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security tests for all sandbox implementations
 */
class AllSandboxSecurityTest {

    private LingoNexusExecutor facade;

    @BeforeEach
    void setUp() {
        // Reset singleton instance to ensure clean state for security tests
        LingoNexusBuilder.resetInstance();

        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .addToBlacklist("java.io.File")
                .build();
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .cacheConfig(CacheConfig.builder().enabled(false).build())  // Disable cache for security tests
                .sandboxConfig(sandboxConfig)
                .build();

        facade = LingoNexusBuilder.loadInstance(config);
    }

    @Test
    @DisplayName("GroovySandbox should block non-whitelisted classes")
    void testGroovySandboxSecurity() {
        // Test with fully qualified class name
        ScriptResult scriptResult = facade.execute("new java.io.File('/tmp')", "groovy", ScriptContext.of(Map.of()));
        assertNotNull(scriptResult.getErrorMessage());
        assertEquals(ExecutionStatus.SECURITY_VIOLATION, scriptResult.getStatus());
        assertTrue(scriptResult.getErrorMessage().contains("blacklisted class") &&
                   scriptResult.getErrorMessage().contains("java.io.File"),
                "Expected error message to contain 'blacklisted class' and 'java.io.File', but was: " + scriptResult.getErrorMessage());
    }

    @Test
    @DisplayName("JavaSandbox should block non-whitelisted classes")
    void testJavaSandboxSecurity() {
        // Test with fully qualified class name
        ScriptResult scriptResult = facade.execute("new java.io.File(\"/tmp\")", "java", ScriptContext.of(Map.of()));
        assertNotNull(scriptResult.getErrorMessage());
        assertTrue(scriptResult.getErrorMessage().contains("blacklisted class") &&
                   scriptResult.getErrorMessage().contains("java.io.File"),
                "Expected error message to contain 'blacklisted class' and 'java.io.File', but was: " + scriptResult.getErrorMessage());
    }

    @Test
    @DisplayName("JavaExprSandbox should block non-whitelisted classes")
    void testJavaExprSandboxSecurity() {
        // Test with fully qualified class name
        ScriptResult scriptResult = facade.execute("new java.io.File(\"/tmp\")", "javaexpr", ScriptContext.of(Map.of()));
        assertNotNull(scriptResult.getErrorMessage());
        assertTrue(scriptResult.getErrorMessage().contains("blacklisted class") &&
                   scriptResult.getErrorMessage().contains("java.io.File"),
                "Expected error message to contain 'blacklisted class' and 'java.io.File', but was: " + scriptResult.getErrorMessage());
    }

    @Test
    @DisplayName("GraalJSSandbox should block non-whitelisted classes")
    void testGraalJSSandboxSecurity() {
        // Test with Java.type
        ScriptResult scriptResult = facade.execute("Java.type('java.io.File')", "js",ScriptContext.of(Map.of()));
        assertNotNull(scriptResult.getErrorMessage());
        assertTrue(scriptResult.getThrowable().getCause().getMessage().contains("Access to host class java.io.File is not allowed"),
                "Expected error message to contain security violation or execution failure, but was: " + scriptResult.getErrorMessage());
    }

    @Test
    @DisplayName("KotlinSandbox should block non-whitelisted classes")
    void testKotlinSandboxSecurity() {
        // Test with fully qualified class name
        ScriptResult scriptResult = facade.execute("java.io.File(\"/tmp\")", "kotlin", ScriptContext.of(Map.of()));
        assertNotNull(scriptResult.getErrorMessage());
        assertTrue(scriptResult.getErrorMessage().contains("blacklisted class") &&
                   scriptResult.getErrorMessage().contains("java.io.File"),
                "Expected error message to contain 'blacklisted class' and 'java.io.File', but was: " + scriptResult.getErrorMessage());
    }
}
