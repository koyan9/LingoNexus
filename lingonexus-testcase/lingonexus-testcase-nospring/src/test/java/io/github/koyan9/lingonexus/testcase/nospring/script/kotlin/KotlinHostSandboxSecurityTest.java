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
import io.github.koyan9.lingonexus.script.kotlin.KotlinHostSandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security tests for KotlinHostSandbox - three-layer security validation
 */
class KotlinHostSandboxSecurityTest {

    @Test
    @DisplayName("Should block blacklisted class at compilation time - java.io.File")
    void shouldBlockBlacklistedClassAtCompileTime() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");
        whitelist.add("kotlin.*");

        Set<String> blacklist = new HashSet<>();
        blacklist.add("java.io.*");

        SandboxConfig config = SandboxConfig.builder()
                .classWhitelist(whitelist)
                .classBlacklist(blacklist)
                .build();
        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(config)
                .build();
        KotlinHostSandbox sandbox = new KotlinHostSandbox(engineConfig);

        String script = "import java.io.File\nval f = File(\"/tmp/test\")";

        assertThrows(ScriptSecurityException.class, () -> {
            sandbox.compile(script, ScriptContext.of(null));
        }, "Should throw ScriptSecurityException for blacklisted class");
    }

    @Test
    @DisplayName("Should block blacklisted class - java.nio.file.Files")
    void shouldBlockNioFiles() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");
        whitelist.add("kotlin.*");

        Set<String> blacklist = new HashSet<>();
        blacklist.add("java.nio.*");

        SandboxConfig config = SandboxConfig.builder()
                .classWhitelist(whitelist)
                .classBlacklist(blacklist)
                .build();

        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(config)
                .build();
        KotlinHostSandbox sandbox = new KotlinHostSandbox(engineConfig);

        String script = "import java.nio.file.Files\nval path = Files.createTempFile(\"test\", \".txt\")";

        assertThrows(ScriptSecurityException.class, () -> {
            sandbox.compile(script, ScriptContext.of(null));
        });
    }

    @Test
    @DisplayName("Should allow whitelisted class - java.util.ArrayList")
    void shouldAllowArrayList() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");
        whitelist.add("kotlin.*");

        Set<String> blacklist = new HashSet<>();

        SandboxConfig config = SandboxConfig.builder()
                .classWhitelist(whitelist)
                .classBlacklist(blacklist)
                .build();

        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(config)
                .build();
        KotlinHostSandbox sandbox = new KotlinHostSandbox(engineConfig);

        String script = "import java.util.ArrayList\nval list = ArrayList<Int>()\nlist.add(1)\nlist.add(2)\nlist.size";

        var compiled = sandbox.compile(script, ScriptContext.of(null));
        Object result = compiled.execute(null);
        assertEquals(2, result);
    }

    @Test
    @DisplayName("Should block blacklisted class even if in whitelist")
    void shouldBlockBlacklistedEvenIfWhitelisted() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.io.*");  // In whitelist
        whitelist.add("kotlin.*");

        Set<String> blacklist = new HashSet<>();
        blacklist.add("java.io.*");  // But also in blacklist - blacklist takes precedence

        SandboxConfig config = SandboxConfig.builder()
                .classWhitelist(whitelist)
                .classBlacklist(blacklist)
                .build();

        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(config)
                .build();
        KotlinHostSandbox sandbox = new KotlinHostSandbox(engineConfig);

        String script = "import java.io.File\nval f = File(\"/tmp\")";

        assertThrows(ScriptSecurityException.class, () -> {
            sandbox.compile(script, ScriptContext.of(null));
        });
    }

    @Test
    @DisplayName("Should block ProcessBuilder")
    void shouldBlockProcessBuilder() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("kotlin.*");

        Set<String> blacklist = new HashSet<>();
        blacklist.add("java.lang.ProcessBuilder");

        SandboxConfig config = SandboxConfig.builder()
                .classWhitelist(whitelist)
                .classBlacklist(blacklist)
                .build();

        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(config)
                .build();
        KotlinHostSandbox sandbox = new KotlinHostSandbox(engineConfig);

        String script = "import java.lang.ProcessBuilder\nval pb = ProcessBuilder(\"ls\")";

        assertThrows(ScriptSecurityException.class, () -> {
            sandbox.compile(script, ScriptContext.of(null));
        });
    }

    // Helper class for module testing
    public static class TestModule {
        public String getValue() {
            return "test-value";
        }

        public String format(String input) {
            return "formatted: " + input;
        }
    }
}
