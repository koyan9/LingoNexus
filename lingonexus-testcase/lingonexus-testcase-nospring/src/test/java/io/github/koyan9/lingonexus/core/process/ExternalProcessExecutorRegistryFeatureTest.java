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
package io.github.koyan9.lingonexus.core.process;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@DisplayName("External Process Executor Registry Feature Tests")
class ExternalProcessExecutorRegistryFeatureTest {

    @AfterEach
    void tearDown() {
        ExternalProcessExecutorRegistry.shutdownAll();
    }

    @Test
    @DisplayName("Should reuse cached executor for identical request signature")
    void shouldReuseCachedExecutorForIdenticalRequestSignature() {
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                createEngineConfig(),
                new DefaultModuleRegistry(),
                Collections.emptyList()
        );

        ExternalProcessExecutionRequest firstRequest = factory.createRequest(
                "return value",
                "groovy",
                ScriptContext.of(Collections.singletonMap("value", 1)),
                Collections.singletonMap("value", 1)
        );
        ExternalProcessExecutionRequest secondRequest = factory.createRequest(
                "return value",
                "groovy",
                ScriptContext.of(Collections.singletonMap("value", 1)),
                Collections.singletonMap("value", 1)
        );

        LingoNexusExecutor firstExecutor = ExternalProcessExecutorRegistry.getOrCreate(firstRequest);
        LingoNexusExecutor secondExecutor = ExternalProcessExecutorRegistry.getOrCreate(secondRequest);
        Map<String, Long> statistics = ExternalProcessExecutorRegistry.getStatistics();

        assertSame(firstExecutor, secondExecutor);
        assertEquals(Long.valueOf(1L), statistics.get("executorCacheSize"));
        assertEquals(Long.valueOf(1L), statistics.get("executorCacheHits"));
        assertEquals(Long.valueOf(1L), statistics.get("executorCacheMisses"));
    }

    private EngineConfig createEngineConfig() {
        return EngineConfig.builder()
                .defaultLanguage("groovy")
                .cacheConfig(CacheConfig.builder().enabled(true).build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(1000L)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .build())
                .build();
    }
}
