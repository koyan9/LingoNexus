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
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.cache.CaffeineCacheProvider;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheKey;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.impl.DefaultSandboxManager;
import io.github.koyan9.lingonexus.core.impl.DefaultScriptExecutor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Default Script Executor Async Feature Tests")
class DefaultScriptExecutorAsyncFeatureTest {

    @Test
    @DisplayName("Should build async error metadata when delegated execute throws")
    void shouldBuildAsyncErrorMetadataWhenDelegatedExecuteThrows() throws Exception {
        DefaultScriptExecutor executor = createThrowingExecutor();

        try {
            CompletableFuture<ScriptResult> future = executor.executeAsync(
                    "return 1",
                    "groovy",
                    ScriptContext.of(Collections.<String, Object>emptyMap())
            );
            ScriptResult result = future.get(5, TimeUnit.SECONDS);

            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals(Boolean.TRUE, result.getMetadata().get("async"));
            assertEquals("IllegalStateException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("simulated async failure", result.getErrorMessage());
            assertTrue(result.isFailure());
        } finally {
            executor.close();
        }
    }

    private DefaultScriptExecutor createThrowingExecutor() {
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

        ScriptCacheManager cacheManager = new ScriptCacheManager(
                new CaffeineCacheProvider<ScriptCacheKey, CompiledScript>(cacheConfig)
        );
        return new ThrowingDefaultScriptExecutor(engineConfig, cacheManager);
    }

    private static final class ThrowingDefaultScriptExecutor extends DefaultScriptExecutor {

        private ThrowingDefaultScriptExecutor(EngineConfig config, ScriptCacheManager cacheManager) {
            super(config, cacheManager);
        }

        @Override
        public ScriptResult execute(String script, String language, ScriptContext context) {
            throw new IllegalStateException("simulated async failure");
        }
    }
}
