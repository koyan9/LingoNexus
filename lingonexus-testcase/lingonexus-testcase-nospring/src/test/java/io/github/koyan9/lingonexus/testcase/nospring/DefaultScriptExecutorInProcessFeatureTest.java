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
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.cache.CaffeineCacheProvider;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheKey;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.impl.DefaultSandboxManager;
import io.github.koyan9.lingonexus.core.impl.DefaultScriptExecutor;
import io.github.koyan9.lingonexus.script.groovy.GroovySandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Script Executor In-Process Feature Tests")
class DefaultScriptExecutorInProcessFeatureTest {

    @Test
    @DisplayName("Should preserve direct execution metadata after stage refactor")
    void shouldPreserveDirectExecutionMetadataAfterStageRefactor() {
        DefaultScriptExecutor executor = createGroovyExecutor();

        try {
            ScriptResult result = executor.execute("return value * 2", "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 21)));

            assertTrue(result.isSuccess());
            assertEquals(42, ((Number) result.getValue()).intValue());

            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata);
            assertEquals("groovy", metadata.get(ResultMetadataKeys.SCRIPT_ENGINE));
            assertEquals(ExecutionIsolationMode.DIRECT.name(), metadata.get(ResultMetadataKeys.ISOLATION_MODE));
            assertNotNull(metadata.get(ResultMetadataKeys.COMPILE_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.CACHE_HIT));
            assertNotNull(metadata.get(ResultMetadataKeys.CACHE_WAIT_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.EXECUTION_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.WALL_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.MODULES_USED));
        } finally {
            executor.close();
        }
    }

    private DefaultScriptExecutor createGroovyExecutor() {
        CacheConfig cacheConfig = CacheConfig.builder().enabled(true).build();
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .enabled(true)
                .timeoutMs(0L)
                .isolationMode(ExecutionIsolationMode.DIRECT)
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
}
