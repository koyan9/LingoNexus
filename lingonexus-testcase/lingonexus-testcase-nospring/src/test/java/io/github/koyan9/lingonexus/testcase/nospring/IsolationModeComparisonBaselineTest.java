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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Isolation Mode Comparison Baseline Tests")
class IsolationModeComparisonBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(IsolationModeComparisonBaselineTest.class);

    @Test
    @DisplayName("Should compare repeated execution baseline across isolation modes")
    void shouldCompareRepeatedExecutionBaselineAcrossIsolationModes() {
        String script = "return (1..500).sum()";
        int iterations = 20;

        BenchmarkSnapshot direct = benchmark(script, iterations, ExecutionIsolationMode.DIRECT, 0L);
        BenchmarkSnapshot isolatedThread = benchmark(script, iterations, ExecutionIsolationMode.ISOLATED_THREAD, 0L);
        BenchmarkSnapshot externalProcess = benchmark(script, iterations, ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L);

        logger.info(
                "Isolation comparison baseline: DIRECT avg={}ms, ISOLATED_THREAD avg={}ms, EXTERNAL_PROCESS cold={}ms avg={}ms, extCacheHits={}",
                String.format("%.2f", direct.averageRepeatedMs),
                String.format("%.2f", isolatedThread.averageRepeatedMs),
                externalProcess.coldDurationMs,
                String.format("%.2f", externalProcess.averageRepeatedMs),
                externalProcess.executorCacheHits
        );
        BenchmarkReportSupport.emit(
                "isolation-mode-comparison",
                "scriptLanguage", "groovy",
                "iterations", iterations,
                "directColdMs", direct.coldDurationMs,
                "directRepeatedTotalMs", direct.repeatedTotalMs,
                "directAvgMs", String.format("%.2f", direct.averageRepeatedMs),
                "directCacheHits", direct.cacheHits,
                "directCacheMisses", direct.cacheMisses,
                "isolatedThreadColdMs", isolatedThread.coldDurationMs,
                "isolatedThreadRepeatedTotalMs", isolatedThread.repeatedTotalMs,
                "isolatedThreadAvgMs", String.format("%.2f", isolatedThread.averageRepeatedMs),
                "isolatedThreadCacheHits", isolatedThread.cacheHits,
                "isolatedThreadCacheMisses", isolatedThread.cacheMisses,
                "externalColdMs", externalProcess.coldDurationMs,
                "externalRepeatedTotalMs", externalProcess.repeatedTotalMs,
                "externalAvgMs", String.format("%.2f", externalProcess.averageRepeatedMs),
                "externalCacheHits", externalProcess.cacheHits,
                "externalCacheMisses", externalProcess.cacheMisses,
                "externalExecutorCacheHits", externalProcess.executorCacheHits
        );

        assertTrue(direct.success);
        assertTrue(isolatedThread.success);
        assertTrue(externalProcess.success);
        assertTrue(externalProcess.executorCacheHits >= 1L);
    }

    private BenchmarkSnapshot benchmark(String script, int iterations, ExecutionIsolationMode mode, long timeoutMs) {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(timeoutMs)
                                .isolationMode(mode)
                                .externalProcessExecutorCacheMaxSize(4)
                                .externalProcessExecutorCacheIdleTtlMs(300000L)
                                .build())
                        .build()
        );

        try {
            long coldStart = System.nanoTime();
            ScriptResult coldResult = executor.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            long coldDurationMs = nanosToMillis(System.nanoTime() - coldStart);

            long repeatedStart = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                ScriptResult result = executor.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
                assertTrue(result.isSuccess());
                assertEquals(125250, ((Number) result.getValue()).intValue());
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
                    repeatedDurationMs / (double) iterations,
                    diagnostics.getStatistics().getCacheHits(),
                    diagnostics.getStatistics().getCacheMisses(),
                    externalExecutorCacheHits
            );
        } finally {
            executor.close();
        }
    }

    private long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static class BenchmarkSnapshot {
        private final boolean success;
        private final long coldDurationMs;
        private final long repeatedTotalMs;
        private final double averageRepeatedMs;
        private final long cacheHits;
        private final long cacheMisses;
        private final long executorCacheHits;

        private BenchmarkSnapshot(boolean success, long coldDurationMs, long repeatedTotalMs, double averageRepeatedMs, long cacheHits, long cacheMisses, long executorCacheHits) {
            this.success = success;
            this.coldDurationMs = coldDurationMs;
            this.repeatedTotalMs = repeatedTotalMs;
            this.averageRepeatedMs = averageRepeatedMs;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
            this.executorCacheHits = executorCacheHits;
        }
    }
}
