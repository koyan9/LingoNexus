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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Security tests for KotlinHostSandbox - static security check validation
 *
 * <p>KotlinHostSandbox uses JSR-223 API and only has static security checks.
 * All security validation happens during compile() method.</p>
 */
class KotlinSandboxSecurityTest {

    @Test
    @DisplayName("Should block blacklisted class during compilation - java.io.File")
    void shouldBlockBlacklistedClass() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");

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
        });
    }

    @Test
    @DisplayName("Should block blacklisted class during compilation - java.nio.file.Files")
    void shouldBlockNioFiles() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");

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
            sandbox.compile(script,ScriptContext.of(null));
        });
    }

    @Test
    @DisplayName("Should allow whitelisted class - java.lang.String")
    void shouldAllowWhitelistedClass() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");

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

        String script = "val s = \"Hello World\"\ns.length";

        // Should compile successfully
        var compiled = sandbox.compile(script, ScriptContext.of(null));
        Object result = compiled.execute(null);
        assertEquals(11, result);
    }

    @Test
    @DisplayName("Should allow whitelisted class - java.util.ArrayList")
    void shouldAllowArrayList() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.util.*");

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

        // Should compile successfully
        var compiled = sandbox.compile(script, ScriptContext.of(null));
        Object result = compiled.execute(null);
        assertEquals(2, result);
    }

    @Test
    @DisplayName("Should block non-whitelisted class during compilation")
    void shouldBlockNonWhitelistedClass() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        // java.util.* is NOT in whitelist

        Set<String> blacklist = new HashSet<>();

        SandboxConfig config = SandboxConfig.builder()
                .classWhitelist(whitelist)
                .classBlacklist(blacklist)
                .build();

        EngineConfig engineConfig = EngineConfig.builder()
                .sandboxConfig(config)
                .build();
        KotlinHostSandbox sandbox = new KotlinHostSandbox(engineConfig);

        String script = "import java.util.ArrayList\nval list = ArrayList<Int>()";

        assertThrows(ScriptSecurityException.class, () -> {
            sandbox.compile(script, ScriptContext.of(null));
        });
    }

    @Test
    @DisplayName("Should block blacklisted class even if in whitelist")
    void shouldBlockBlacklistedEvenIfWhitelisted() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");
        whitelist.add("java.io.*");  // In whitelist

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
    @DisplayName("Should block ProcessBuilder during compilation")
    void shouldBlockProcessBuilder() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");

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

    @Test
    @DisplayName("Should work with compile method - block blacklisted class")
    void shouldBlockInCompileMethod() {
        Set<String> whitelist = new HashSet<>();
        whitelist.add("java.lang.*");

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

        String script = "import java.io.File\nval f = File(\"/tmp\")";

        assertThrows(ScriptSecurityException.class, () -> {
            sandbox.compile(script, ScriptContext.of(null));
        });
    }
}
