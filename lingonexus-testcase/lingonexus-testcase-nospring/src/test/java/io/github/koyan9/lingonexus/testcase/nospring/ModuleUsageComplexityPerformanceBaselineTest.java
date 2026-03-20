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

@DisplayName("Module Usage Complexity Performance Baseline Tests")
class ModuleUsageComplexityPerformanceBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(ModuleUsageComplexityPerformanceBaselineTest.class);

    @Test
    @DisplayName("Should compare simple arithmetic and module-heavy direct execution under similar context size")
    void shouldCompareSimpleArithmeticAndModuleHeavyDirectExecutionUnderSimilarContextSize() {
        BenchmarkScenario arithmeticScenario = new BenchmarkScenario("simple-arithmetic", 48, 48, 64, 0, 20);
        BenchmarkScenario moduleHeavyScenario = new BenchmarkScenario("module-heavy", 48, 48, 64, 24, 20);

        BenchmarkSnapshot arithmetic = benchmark(arithmeticScenario, createArithmeticScript());
        BenchmarkSnapshot moduleHeavy = benchmark(moduleHeavyScenario, createModuleHeavyScript(moduleHeavyScenario.moduleCount));

        logger.info(
                "Module-usage baseline: arithmetic cold={}ms avg={}ms, moduleHeavy cold={}ms avg={}ms, arithmeticCacheHits={}, moduleHeavyCacheHits={}",
                arithmetic.coldDurationMs,
                String.format("%.2f", arithmetic.averageRepeatedMs),
                moduleHeavy.coldDurationMs,
                String.format("%.2f", moduleHeavy.averageRepeatedMs),
                arithmetic.cacheHits,
                moduleHeavy.cacheHits
        );
        BenchmarkReportSupport.emit(
                "module-usage-direct",
                "scriptLanguage", "groovy",
                "isolationMode", ExecutionIsolationMode.DIRECT.name(),
                "iterations", arithmeticScenario.iterations,
                "globals", arithmeticScenario.globalVariableCount,
                "requestVars", arithmeticScenario.requestVariableCount,
                "metadata", arithmeticScenario.metadataCount,
                "arithmeticModules", arithmeticScenario.moduleCount,
                "arithmeticColdMs", arithmetic.coldDurationMs,
                "arithmeticRepeatedTotalMs", arithmetic.repeatedTotalMs,
                "arithmeticAvgMs", String.format("%.2f", arithmetic.averageRepeatedMs),
                "arithmeticCacheHits", arithmetic.cacheHits,
                "arithmeticCacheMisses", arithmetic.cacheMisses,
                "moduleHeavyModules", moduleHeavyScenario.moduleCount,
                "moduleHeavyColdMs", moduleHeavy.coldDurationMs,
                "moduleHeavyRepeatedTotalMs", moduleHeavy.repeatedTotalMs,
                "moduleHeavyAvgMs", String.format("%.2f", moduleHeavy.averageRepeatedMs),
                "moduleHeavyCacheHits", moduleHeavy.cacheHits,
                "moduleHeavyCacheMisses", moduleHeavy.cacheMisses
        );

        assertEquals(arithmeticScenario.iterations, arithmetic.cacheHits);
        assertEquals(moduleHeavyScenario.iterations, moduleHeavy.cacheHits);
        assertEquals(1L, arithmetic.cacheMisses);
        assertEquals(1L, moduleHeavy.cacheMisses);
    }

    private BenchmarkSnapshot benchmark(BenchmarkScenario scenario, String script) {
        DefaultScriptExecutor executor = createGroovyExecutor(scenario);
        ScriptContext context = createRequestContext(scenario);
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
                    coldDurationMs,
                    repeatedDurationMs,
                    repeatedDurationMs / (double) scenario.iterations,
                    statistics.getCacheHits(),
                    statistics.getCacheMisses()
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
        variableManager.setVariable("globalAnchor", 21);
        for (int i = 0; i < scenario.globalVariableCount; i++) {
            variableManager.setVariable("globalExtra" + i, i);
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
        variables.put("requestAnchor", 21);
        for (int i = 0; i < scenario.requestVariableCount; i++) {
            variables.put("requestExtra" + i, i);
        }

        Map<String, Object> metadata = new HashMap<String, Object>();
        for (int i = 0; i < scenario.metadataCount; i++) {
            metadata.put("metadataKey" + i, "metadataValue" + i);
        }
        return ScriptContext.of(variables, metadata);
    }

    private String createArithmeticScript() {
        return "return globalAnchor + requestAnchor";
    }

    private String createModuleHeavyScript(int moduleCount) {
        StringBuilder script = new StringBuilder("return globalAnchor + requestAnchor");
        for (int i = 0; i < moduleCount; i++) {
            script.append(" + (module").append(i).append(" != null ? 1 : 0)");
        }
        return script.toString();
    }

    private int expectedResult(BenchmarkScenario scenario) {
        return 42 + scenario.moduleCount;
    }

    private long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static final class BenchmarkScenario {
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

    private static final class BenchmarkSnapshot {
        private final long coldDurationMs;
        private final long repeatedTotalMs;
        private final double averageRepeatedMs;
        private final long cacheHits;
        private final long cacheMisses;

        private BenchmarkSnapshot(long coldDurationMs, long repeatedTotalMs, double averageRepeatedMs,
                                  long cacheHits, long cacheMisses) {
            this.coldDurationMs = coldDurationMs;
            this.repeatedTotalMs = repeatedTotalMs;
            this.averageRepeatedMs = averageRepeatedMs;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
        }
    }

    private static final class BenchmarkModule implements ScriptModule {
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
