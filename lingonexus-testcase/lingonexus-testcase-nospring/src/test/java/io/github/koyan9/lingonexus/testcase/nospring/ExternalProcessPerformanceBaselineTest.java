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

@DisplayName("External Process Performance Baseline Tests")
class ExternalProcessPerformanceBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(ExternalProcessPerformanceBaselineTest.class);

    @Test
    @DisplayName("Should establish repeated-execution baseline for external worker reuse")
    void shouldEstablishRepeatedExecutionBaselineForExternalWorkerReuse() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .externalProcessExecutorCacheMaxSize(4)
                                .externalProcessExecutorCacheIdleTtlMs(300000L)
                                .build())
                        .build()
        );

        try {
            String script = "return (1..500).sum()";

            long coldStart = System.nanoTime();
            ScriptResult coldResult = executor.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            long coldDurationMs = nanosToMillis(System.nanoTime() - coldStart);

            long repeatedStart = System.nanoTime();
            int iterations = 20;
            for (int i = 0; i < iterations; i++) {
                ScriptResult result = executor.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
                assertTrue(result.isSuccess());
                assertEquals(125250, ((Number) result.getValue()).intValue());
            }
            long repeatedDurationMs = nanosToMillis(System.nanoTime() - repeatedStart);

            EngineDiagnostics diagnostics = executor.getDiagnostics();

            assertTrue(coldResult.isSuccess());
            assertEquals(125250, ((Number) coldResult.getValue()).intValue());
            assertTrue(diagnostics.getExternalProcessStatistics().getExecutorCacheHits() >= 1);

            double averageRepeatedMs = repeatedDurationMs / (double) iterations;
            logger.info(
                    "External-process baseline: cold={}ms, repeatedTotal={}ms, repeatedAvg={}ms, executorCacheHits={}, executorCacheMisses={}",
                    coldDurationMs,
                    repeatedDurationMs,
                    String.format("%.2f", averageRepeatedMs),
                    diagnostics.getExternalProcessStatistics().getExecutorCacheHits(),
                    diagnostics.getExternalProcessStatistics().getExecutorCacheMisses()
            );
            BenchmarkReportSupport.emit(
                    "external-process-reuse",
                    "scriptLanguage", "groovy",
                    "isolationMode", ExecutionIsolationMode.EXTERNAL_PROCESS.name(),
                    "iterations", iterations,
                    "coldMs", coldDurationMs,
                    "repeatedTotalMs", repeatedDurationMs,
                    "repeatedAvgMs", String.format("%.2f", averageRepeatedMs),
                    "executorCacheHits", diagnostics.getExternalProcessStatistics().getExecutorCacheHits(),
                    "executorCacheMisses", diagnostics.getExternalProcessStatistics().getExecutorCacheMisses(),
                    "borrowCount", diagnostics.getExternalProcessStatistics().getBorrowCount(),
                    "discardCount", diagnostics.getExternalProcessStatistics().getDiscardCount(),
                    "evictionCount", diagnostics.getExternalProcessStatistics().getEvictionCount()
            );
        } finally {
            executor.close();
        }
    }

    private long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }
}
