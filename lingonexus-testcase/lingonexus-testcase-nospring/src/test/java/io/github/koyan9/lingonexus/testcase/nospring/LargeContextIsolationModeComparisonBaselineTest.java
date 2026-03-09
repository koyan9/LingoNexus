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

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import io.github.koyan9.lingonexus.core.context.GlobalVariableManager;
import io.github.koyan9.lingonexus.testcase.nospring.support.ExternalProcessDescriptorBenchmarkModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Large Context Isolation Mode Comparison Baseline Tests")
class LargeContextIsolationModeComparisonBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(LargeContextIsolationModeComparisonBaselineTest.class);

    @Test
    @DisplayName("Should compare large-context repeated execution across isolation modes")
    void shouldCompareLargeContextRepeatedExecutionAcrossIsolationModes() {
        BenchmarkScenario scenario = new BenchmarkScenario(300, 300, 300, 20, 10);

        BenchmarkSnapshot direct = benchmark(scenario, ExecutionIsolationMode.DIRECT, 0L);
        BenchmarkSnapshot isolatedThread = benchmark(scenario, ExecutionIsolationMode.ISOLATED_THREAD, 0L);
        BenchmarkSnapshot externalProcess = benchmark(scenario, ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L);

        logger.info(
                "Large-context isolation comparison: DIRECT cold={}ms avg={}ms, ISOLATED_THREAD cold={}ms avg={}ms, EXTERNAL_PROCESS cold={}ms avg={}ms, extExecutorCacheHits={}",
                direct.coldDurationMs,
                String.format("%.2f", direct.averageRepeatedMs),
                isolatedThread.coldDurationMs,
                String.format("%.2f", isolatedThread.averageRepeatedMs),
                externalProcess.coldDurationMs,
                String.format("%.2f", externalProcess.averageRepeatedMs),
                externalProcess.externalExecutorCacheHits
        );
        BenchmarkReportSupport.emit(
                "large-context-isolation-comparison",
                "scriptLanguage", "groovy",
                "globals", scenario.globalVariableCount,
                "requestVars", scenario.requestVariableCount,
                "metadata", scenario.metadataCount,
                "modules", scenario.moduleCount,
                "iterations", scenario.iterations,
                "directColdMs", direct.coldDurationMs,
                "directRepeatedTotalMs", direct.repeatedTotalMs,
                "directAvgMs", String.format("%.2f", direct.averageRepeatedMs),
                "directCacheHits", direct.cacheHits,
                "directCacheMisses", direct.cacheMisses,
                "isolatedColdMs", isolatedThread.coldDurationMs,
                "isolatedRepeatedTotalMs", isolatedThread.repeatedTotalMs,
                "isolatedAvgMs", String.format("%.2f", isolatedThread.averageRepeatedMs),
                "isolatedCacheHits", isolatedThread.cacheHits,
                "isolatedCacheMisses", isolatedThread.cacheMisses,
                "externalColdMs", externalProcess.coldDurationMs,
                "externalRepeatedTotalMs", externalProcess.repeatedTotalMs,
                "externalAvgMs", String.format("%.2f", externalProcess.averageRepeatedMs),
                "externalCacheHits", externalProcess.cacheHits,
                "externalCacheMisses", externalProcess.cacheMisses,
                "externalExecutorCacheHits", externalProcess.externalExecutorCacheHits
        );

        assertTrue(direct.success);
        assertTrue(isolatedThread.success);
        assertTrue(externalProcess.success);
        assertEquals(scenario.iterations, direct.cacheHits);
        assertEquals(scenario.iterations, isolatedThread.cacheHits);
        assertEquals(scenario.iterations, externalProcess.cacheHits);
        assertEquals(1L, direct.cacheMisses);
        assertEquals(1L, isolatedThread.cacheMisses);
        assertEquals(1L, externalProcess.cacheMisses);
        assertTrue(externalProcess.externalExecutorCacheHits >= 1L);
    }

    private BenchmarkSnapshot benchmark(BenchmarkScenario scenario, ExecutionIsolationMode mode, long timeoutMs) {
        GlobalVariableManager variableManager = new GlobalVariableManager();
        for (int i = 0; i < scenario.globalVariableCount; i++) {
            variableManager.setVariable("globalValue" + i, i);
        }

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .variableManager(variableManager)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(timeoutMs)
                                .isolationMode(mode)
                                .externalProcessPoolSize(1)
                                .externalProcessExecutorCacheMaxSize(4)
                                .externalProcessExecutorCacheIdleTtlMs(300000L)
                                .build())
                        .build()
        );

        for (int i = 0; i < scenario.moduleCount; i++) {
            executor.registerModule(new ExternalProcessDescriptorBenchmarkModule("module" + i));
        }

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
            EngineDiagnostics diagnostics = executor.getDiagnostics();

            long externalExecutorCacheHits = diagnostics.getExternalProcessStatistics() != null
                    ? diagnostics.getExternalProcessStatistics().getExecutorCacheHits()
                    : 0L;

            return new BenchmarkSnapshot(
                    coldResult.isSuccess(),
                    coldDurationMs,
                    repeatedDurationMs,
                    repeatedDurationMs / (double) scenario.iterations,
                    diagnostics.getStatistics().getCacheHits(),
                    diagnostics.getStatistics().getCacheMisses(),
                    externalExecutorCacheHits
            );
        } finally {
            executor.close();
        }
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
        private final int globalVariableCount;
        private final int requestVariableCount;
        private final int metadataCount;
        private final int moduleCount;
        private final int iterations;

        private BenchmarkScenario(int globalVariableCount, int requestVariableCount,
                                  int metadataCount, int moduleCount, int iterations) {
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
        private final long externalExecutorCacheHits;

        private BenchmarkSnapshot(boolean success, long coldDurationMs, long repeatedTotalMs, double averageRepeatedMs,
                                  long cacheHits, long cacheMisses, long externalExecutorCacheHits) {
            this.success = success;
            this.coldDurationMs = coldDurationMs;
            this.repeatedTotalMs = repeatedTotalMs;
            this.averageRepeatedMs = averageRepeatedMs;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.externalExecutorCacheHits = externalExecutorCacheHits;
        }
    }
}
