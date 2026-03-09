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
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;
import io.github.koyan9.lingonexus.core.cache.CaffeineCacheProvider;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheKey;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.executor.ExecutionStatisticsCollector;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.impl.DefaultSandboxManager;
import io.github.koyan9.lingonexus.core.impl.DefaultScriptExecutor;
import io.github.koyan9.lingonexus.script.groovy.GroovySandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Execution Statistics Feature Tests")
class ExecutionStatisticsFeatureTest {

    @Test
    @DisplayName("Should not treat cache-disabled execution as cache miss")
    void shouldNotTreatCacheDisabledExecutionAsCacheMiss() {
        DefaultScriptExecutor executor = createGroovyExecutor(false);

        try {
            assertTrue(executor.execute("return 1 + 1", "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());

            EngineStatistics statistics = executor.getStatistics();
            assertEquals(1L, statistics.getTotalExecutions());
            assertEquals(0L, statistics.getCacheHits());
            assertEquals(0L, statistics.getCacheMisses());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should record direct-mode cache hit after warm execution")
    void shouldRecordDirectModeCacheHitAfterWarmExecution() {
        DefaultScriptExecutor executor = createGroovyExecutor(true);

        try {
            String script = "return 6 * 7";
            assertTrue(executor.execute(script, "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());
            assertTrue(executor.execute(script, "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());

            EngineStatistics statistics = executor.getStatistics();
            assertEquals(2L, statistics.getTotalExecutions());
            assertEquals(1L, statistics.getCacheHits());
            assertEquals(1L, statistics.getCacheMisses());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should count only explicit cache outcomes")
    void shouldCountOnlyExplicitCacheOutcomes() {
        ExecutionStatisticsCollector collector = new ExecutionStatisticsCollector();

        collector.record("groovy", true, null, 3L);
        collector.record("groovy", true, Boolean.FALSE, 5L);
        collector.record("groovy", true, Boolean.TRUE, 7L);

        EngineStatistics statistics = collector.snapshot();
        assertEquals(3L, statistics.getTotalExecutions());
        assertEquals(3L, statistics.getSuccessfulExecutions());
        assertEquals(1L, statistics.getCacheHits());
        assertEquals(1L, statistics.getCacheMisses());
        assertEquals(5L, statistics.getAverageExecutionTimeMs());
        assertEquals(3L, statistics.getExecutionsByLanguage().get("groovy").longValue());
    }

    private DefaultScriptExecutor createGroovyExecutor(boolean cacheEnabled) {
        CacheConfig cacheConfig = CacheConfig.builder().enabled(cacheEnabled).build();
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