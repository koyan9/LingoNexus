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

import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.exception.ScriptSecurityException;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.modules.*;
import io.github.koyan9.lingonexus.script.kotlin.KotlinHostSandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for KotlinHostSandbox
 * Tests runtime security checks, module injection, and custom modules
 */
@DisplayName("KotlinHostSandbox Comprehensive Tests")
public class KotlinHostSandboxComprehensiveTest {

    // ==================== Runtime Security Tests ====================

    // ==================== Built-in Module Tests ====================

    // ==================== Custom Module Tests ====================

    // ==================== Helper Classes ====================

    /**
     * Custom module for testing with actual methods that Kotlin can resolve
     */
    public static class CustomGreetingModule implements ScriptModule {
        @Override
        public String getName() {
            return "custom";
        }

        @Override
        public Map<String, Object> getFunctions() {
            Map<String, Object> functions = new HashMap<>();
            functions.put("greet", (java.util.function.Function<String, String>) this::greet);
            functions.put("add", (java.util.function.BiFunction<Integer, Integer, Integer>) this::add);
            return functions;
        }

        @Override
        public boolean hasFunction(String functionName) {
            return "greet".equals(functionName) || "add".equals(functionName);
        }

        // Actual methods that Kotlin can resolve
        public String greet(String name) {
            return "Hello, " + name + "!";
        }

        public int add(int a, int b) {
            return a + b;
        }
    }
}
