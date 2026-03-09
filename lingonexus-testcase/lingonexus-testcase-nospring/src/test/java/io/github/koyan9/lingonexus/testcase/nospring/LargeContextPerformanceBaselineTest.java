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
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;
import io.github.koyan9.lingonexus.core.cache.CaffeineCacheProvider;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheKey;
import io.github.koyan9.lingonexus.core.cache.ScriptCacheManager;
import io.github.koyan9.lingonexus.core.context.GlobalVariableManager;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import io.github.koyan9.lingonexus.core.impl.DefaultSandboxManager;
import io.github.koyan9.lingonexus.core.impl.DefaultScriptExecutor;
import io.github.koyan9.lingonexus.script.groovy.GroovySandbox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Large Context Performance Baseline Tests")
class LargeContextPerformanceBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(LargeContextPerformanceBaselineTest.class);

    @Test
    @DisplayName("Should establish direct-execution baseline for large contexts and module-heavy requests")
    void shouldEstablishDirectExecutionBaselineForLargeContextsAndModuleHeavyRequests() {
        BenchmarkScenario smallScenario = new BenchmarkScenario("small", 16, 16, 16, 2, 20);
        BenchmarkScenario largeScenario = new BenchmarkScenario("large", 1000, 1000, 1000, 100, 20);

        BenchmarkSnapshot small = benchmark(smallScenario);
        BenchmarkSnapshot large = benchmark(largeScenario);

        logger.info(
                "Large-context baseline: {} cold={}ms avg={}ms (globals={}, requestVars={}, metadata={}, modules={}), {} cold={}ms avg={}ms (globals={}, requestVars={}, metadata={}, modules={}), smallCacheHits={}, largeCacheHits={}",
                smallScenario.name,
                small.coldDurationMs,
                String.format("%.2f", small.averageRepeatedMs),
                smallScenario.globalVariableCount,
                smallScenario.requestVariableCount,
                smallScenario.metadataCount,
                smallScenario.moduleCount,
                largeScenario.name,
                large.coldDurationMs,
                String.format("%.2f", large.averageRepeatedMs),
                largeScenario.globalVariableCount,
                largeScenario.requestVariableCount,
                largeScenario.metadataCount,
                largeScenario.moduleCount,
                small.cacheHits,
                large.cacheHits
        );
        BenchmarkReportSupport.emit(
                "large-context-direct",
                "scriptLanguage", "groovy",
                "isolationMode", ExecutionIsolationMode.DIRECT.name(),
                "smallGlobals", smallScenario.globalVariableCount,
                "smallRequestVars", smallScenario.requestVariableCount,
                "smallMetadata", smallScenario.metadataCount,
                "smallModules", smallScenario.moduleCount,
                "smallIterations", smallScenario.iterations,
                "smallColdMs", small.coldDurationMs,
                "smallRepeatedTotalMs", small.repeatedTotalMs,
                "smallAvgMs", String.format("%.2f", small.averageRepeatedMs),
                "smallCacheHits", small.cacheHits,
                "smallCacheMisses", small.cacheMisses,
                "smallTotalExecutions", small.totalExecutions,
                "largeGlobals", largeScenario.globalVariableCount,
                "largeRequestVars", largeScenario.requestVariableCount,
                "largeMetadata", largeScenario.metadataCount,
                "largeModules", largeScenario.moduleCount,
                "largeIterations", largeScenario.iterations,
                "largeColdMs", large.coldDurationMs,
                "largeRepeatedTotalMs", large.repeatedTotalMs,
                "largeAvgMs", String.format("%.2f", large.averageRepeatedMs),
                "largeCacheHits", large.cacheHits,
                "largeCacheMisses", large.cacheMisses,
                "largeTotalExecutions", large.totalExecutions
        );

        assertTrue(small.success);
        assertTrue(large.success);
        assertEquals(smallScenario.iterations, small.cacheHits);
        assertEquals(largeScenario.iterations, large.cacheHits);
        assertEquals(1L, small.cacheMisses);
        assertEquals(1L, large.cacheMisses);
        assertEquals(smallScenario.iterations + 1L, small.totalExecutions);
        assertEquals(largeScenario.iterations + 1L, large.totalExecutions);
    }

    private BenchmarkSnapshot benchmark(BenchmarkScenario scenario) {
        DefaultScriptExecutor executor = createGroovyExecutor(scenario);
        ScriptContext context = createRequestContext(scenario);
        String script = createScript(scenario);
        int expectedValue = expectedResult(scenario);

        try {
            long coldStart = System.nanoTime();
            ScriptResult coldResult = executor.execute(script, ScriptLanguage.GROOVY.getId(), context);
            long coldDurationMs = nanosToMillis(System.nanoTime() - coldStart);
            assertTrue(coldResult.isSuccess());
            assertEquals(expectedValue, ((Number) coldResult.getValue()).intValue());

            long repeatedStart = System.nanoTime();
            for (int i = 0; i < scenario.iterations; i++) {
                ScriptResult result = executor.execute(script, ScriptLanguage.GROOVY.getId(), context);
                assertTrue(result.isSuccess());
                assertEquals(expectedValue, ((Number) result.getValue()).intValue());
            }
            long repeatedDurationMs = nanosToMillis(System.nanoTime() - repeatedStart);
            EngineStatistics statistics = executor.getStatistics();

            return new BenchmarkSnapshot(
                    coldResult.isSuccess(),
                    coldDurationMs,
                    repeatedDurationMs,
                    repeatedDurationMs / (double) scenario.iterations,
                    statistics.getCacheHits(),
                    statistics.getCacheMisses(),
                    statistics.getTotalExecutions()
            );
        } finally {
            executor.close();
        }
    }

    private DefaultScriptExecutor createGroovyExecutor(BenchmarkScenario scenario) {
        CacheConfig cacheConfig = CacheConfig.builder().enabled(true).build();
        SandboxConfig sandboxConfig = SandboxConfig.builder()
                .enabled(true)
                .timeoutMs(0L)
                .isolationMode(ExecutionIsolationMode.DIRECT)
                .build();

        GlobalVariableManager variableManager = new GlobalVariableManager();
        for (int i = 0; i < scenario.globalVariableCount; i++) {
            variableManager.setVariable("globalValue" + i, i);
        }

        EngineConfig engineConfig = EngineConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY.getId())
                .cacheConfig(cacheConfig)
                .sandboxConfig(sandboxConfig)
                .variableManager(variableManager)
                .build();

        DefaultSandboxManager sandboxManager = new DefaultSandboxManager();
        DefaultModuleRegistry moduleRegistry = new DefaultModuleRegistry();
        for (int i = 0; i < scenario.moduleCount; i++) {
            moduleRegistry.registerModule(new BenchmarkModule("module" + i));
        }
        engineConfig.setSandboxManager(sandboxManager);
        engineConfig.setModuleRegistry(moduleRegistry);
        sandboxManager.registerSandbox(new GroovySandbox(engineConfig));

        ScriptCacheManager cacheManager = new ScriptCacheManager(
                new CaffeineCacheProvider<ScriptCacheKey, CompiledScript>(cacheConfig)
        );
        return new DefaultScriptExecutor(engineConfig, cacheManager);
    }

    private ScriptContext createRequestContext(BenchmarkScenario scenario) {
        Map<String, Object> variables = new HashMap<String, Object>();
        for (int i = 0; i < scenario.requestVariableCount; i++) {
            variables.put("requestValue" + i, i);
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        for (int i = 0; i < scenario.metadataCount; i++) {
            metadata.put("metadataKey" + i, "metadataValue" + i);
        }
        return ScriptContext.of(variables, metadata);
    }

    private String createScript(BenchmarkScenario scenario) {
        return "return globalValue" + (scenario.globalVariableCount - 1)
                + " + requestValue" + (scenario.requestVariableCount - 1)
                + " + (module" + (scenario.moduleCount - 1) + " != null ? 1 : 0)";
    }

    private int expectedResult(BenchmarkScenario scenario) {
        return (scenario.globalVariableCount - 1) + (scenario.requestVariableCount - 1) + 1;
    }

    private long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static class BenchmarkScenario {
        private final String name;
        private final int globalVariableCount;
        private final int requestVariableCount;
        private final int metadataCount;
        private final int moduleCount;
        private final int iterations;

        private BenchmarkScenario(String name, int globalVariableCount, int requestVariableCount,
                                  int metadataCount, int moduleCount, int iterations) {
            this.name = name;
            this.globalVariableCount = globalVariableCount;
            this.requestVariableCount = requestVariableCount;
            this.metadataCount = metadataCount;
            this.moduleCount = moduleCount;
            this.iterations = iterations;
        }
    }

    private static class BenchmarkSnapshot {
        private final boolean success;
        private final long coldDurationMs;
        private final long repeatedTotalMs;
        private final double averageRepeatedMs;
        private final long cacheHits;
        private final long cacheMisses;
        private final long totalExecutions;

        private BenchmarkSnapshot(boolean success, long coldDurationMs, long repeatedTotalMs, double averageRepeatedMs,
                                  long cacheHits, long cacheMisses, long totalExecutions) {
            this.success = success;
            this.coldDurationMs = coldDurationMs;
            this.repeatedTotalMs = repeatedTotalMs;
            this.averageRepeatedMs = averageRepeatedMs;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.totalExecutions = totalExecutions;
        }
    }

    private static class BenchmarkModule implements ScriptModule {
        private final String name;

        private BenchmarkModule(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Map<String, Object> getFunctions() {
            return Collections.emptyMap();
        }

        @Override
        public boolean hasFunction(String functionName) {
            return false;
        }
    }
}
