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

import io.github.koyan9.lingonexus.api.compiled.CompiledScript;
import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.core.cache.CaffeineCacheProvider;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheKey;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.impl.DefaultSandboxManager;
import io.github.koyan9.lingonexus.core.impl.DefaultScriptExecutor;
import io.github.koyan9.lingonexus.script.groovy.GroovySandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Script Executor Lazy External Runtime Feature Tests")
class DefaultScriptExecutorLazyExternalRuntimeFeatureTest {

    @Test
    @DisplayName("Should avoid external runtime initialization for direct mode")
    void shouldAvoidExternalRuntimeInitializationForDirectMode() throws Exception {
        DefaultScriptExecutor executor = createGroovyExecutor(ExecutionIsolationMode.DIRECT);

        try {
            assertNull(getExternalProcessRuntime(executor));

            EngineDiagnostics diagnostics = executor.getDiagnostics();
            assertNull(getExternalProcessRuntime(executor));
            assertEquals(0, diagnostics.getExternalProcessStatistics().getCreatedWorkers());
            assertNull(diagnostics.getExternalProcessStatistics().getWorkerProtocolVersion());

            assertTrue(executor.execute("return 1 + 1", "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());
            assertNull(getExternalProcessRuntime(executor));
        } finally {
            executor.close();
        }
    }

    private DefaultScriptExecutor createGroovyExecutor(ExecutionIsolationMode isolationMode) {
        CacheConfig cacheConfig = CacheConfig.builder().enabled(true).build();
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .enabled(true)
                .timeoutMs(0L)
                .isolationMode(isolationMode)
                .build();
        EngineConfig engineConfig = EngineConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY.getId())
                .cacheConfig(cacheConfig)
                .sandboxConfig(sandboxConfig)
                .build();

        DefaultSandboxManager sandboxManager = new DefaultSandboxManager();
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        engineConfig.setSandboxManager(sandboxManager);
        engineConfig.setModuleRegistry(moduleRegistry);
        sandboxManager.registerSandbox(new GroovySandbox(engineConfig));

        ScriptCacheManager cacheManager = new ScriptCacheManager(
                new CaffeineCacheProvider<ScriptCacheKey, CompiledScript>(cacheConfig)
        );
        return new DefaultScriptExecutor(engineConfig, cacheManager);
    }

    private Object getExternalProcessRuntime(DefaultScriptExecutor executor) throws Exception {
        Field field = DefaultScriptExecutor.class.getDeclaredField("externalProcessRuntime");
        field.setAccessible(true);
        return field.get(executor);
    }
}
