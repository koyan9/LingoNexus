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

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Java Janino Cache Identity Performance Baseline Tests")
class JavaJaninoCacheIdentityPerformanceBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(JavaJaninoCacheIdentityPerformanceBaselineTest.class);

    @Test
    @DisplayName("Should establish baseline for stable-type and alternating-type Java cache reuse")
    void shouldEstablishBaselineForStableTypeAndAlternatingTypeJavaCacheReuse() {
        String script = "value != null";
        int iterations = 20;

        BenchmarkSnapshot stable = benchmarkStableIntegerShape(script, iterations);
        BenchmarkSnapshot alternating = benchmarkAlternatingShapes(script, iterations);

        logger.info(
                "Janino cache-identity baseline: stable cold={}ms avg={}ms hits={}, alternating warmup={}ms avg={}ms hits={}",
                stable.coldDurationMs,
                String.format("%.2f", stable.averageRepeatedMs),
                stable.cacheHits,
                alternating.coldDurationMs,
                String.format("%.2f", alternating.averageRepeatedMs),
                alternating.cacheHits
        );
        BenchmarkReportSupport.emit(
                "janino-cache-identity",
                "scriptLanguage", "java",
                "scriptType", "janino-expression",
                "contextTypeShapePattern", "stable-integer-vs-alternating-integer-string",
                "iterations", iterations,
                "stableColdMs", stable.coldDurationMs,
                "stableRepeatedTotalMs", stable.repeatedTotalMs,
                "stableAvgMs", String.format("%.2f", stable.averageRepeatedMs),
                "stableCacheHits", stable.cacheHits,
                "stableCacheMisses", stable.cacheMisses,
                "alternatingWarmupMs", alternating.coldDurationMs,
                "alternatingRepeatedTotalMs", alternating.repeatedTotalMs,
                "alternatingAvgMs", String.format("%.2f", alternating.averageRepeatedMs),
                "alternatingCacheHits", alternating.cacheHits,
                "alternatingCacheMisses", alternating.cacheMisses
        );

        assertEquals(iterations, stable.cacheHits);
        assertEquals(iterations * 2L, alternating.cacheHits);
    }

    private BenchmarkSnapshot benchmarkStableIntegerShape(String script, int iterations) {
        LingoNexusExecutor executor = createJavaExecutor();
        try {
            ScriptResult cold = executor.execute(script, "java",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", Integer.valueOf(1))));
            assertTrue(cold.isSuccess());
            assertFalse((Boolean) cold.getMetadata().get(ResultMetadataKeys.CACHE_HIT));

            long repeatedStart = System.nanoTime();
            long cacheHits = 0L;
            for (int i = 0; i < iterations; i++) {
                ScriptResult result = executor.execute(script, "java",
                        ScriptContext.of(Collections.<String, Object>singletonMap("value", Integer.valueOf(i + 2))));
                assertTrue(result.isSuccess());
                assertEquals(Boolean.TRUE, result.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
                cacheHits++;
            }
            long repeatedDurationMs = nanosToMillis(System.nanoTime() - repeatedStart);
            return new BenchmarkSnapshot(
                    cold.getExecutionTime(),
                    repeatedDurationMs,
                    repeatedDurationMs / (double) iterations,
                    cacheHits,
                    1L
            );
        } finally {
            executor.close();
        }
    }

    private BenchmarkSnapshot benchmarkAlternatingShapes(String script, int iterations) {
        LingoNexusExecutor executor = createJavaExecutor();
        try {
            ScriptResult coldInteger = executor.execute(script, "java",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", Integer.valueOf(1))));
            ScriptResult coldString = executor.execute(script, "java",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", "one")));
            assertTrue(coldInteger.isSuccess());
            assertTrue(coldString.isSuccess());
            assertFalse((Boolean) coldInteger.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
            assertFalse((Boolean) coldString.getMetadata().get(ResultMetadataKeys.CACHE_HIT));

            long repeatedStart = System.nanoTime();
            long cacheHits = 0L;
            for (int i = 0; i < iterations; i++) {
                ScriptResult intResult = executor.execute(script, "java",
                        ScriptContext.of(Collections.<String, Object>singletonMap("value", Integer.valueOf(i + 2))));
                ScriptResult stringResult = executor.execute(script, "java",
                        ScriptContext.of(Collections.<String, Object>singletonMap("value", "value-" + i)));
                assertTrue(intResult.isSuccess());
                assertTrue(stringResult.isSuccess());
                assertEquals(Boolean.TRUE, intResult.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
                assertEquals(Boolean.TRUE, stringResult.getMetadata().get(ResultMetadataKeys.CACHE_HIT));
                cacheHits += 2L;
            }
            long repeatedDurationMs = nanosToMillis(System.nanoTime() - repeatedStart);
            return new BenchmarkSnapshot(
                    coldInteger.getExecutionTime() + coldString.getExecutionTime(),
                    repeatedDurationMs,
                    repeatedDurationMs / (double) (iterations * 2),
                    cacheHits,
                    2L
            );
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createJavaExecutor() {
        return io.github.koyan9.lingonexus.core.LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.JAVA)
                        .allowedSandboxLanguage(ScriptLanguage.JAVA)
                        .cacheConfig(CacheConfig.builder().enabled(true).build())
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(0L)
                                .isolationMode(ExecutionIsolationMode.DIRECT)
                                .build())
                        .build()
        );
    }

    private long nanosToMillis(long nanos) {
        return java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    private static final class BenchmarkSnapshot {
        private final long coldDurationMs;
        private final long repeatedTotalMs;
        private final double averageRepeatedMs;
        private final long cacheHits;
        private final long cacheMisses;

        private BenchmarkSnapshot(long coldDurationMs, long repeatedTotalMs, double averageRepeatedMs, long cacheHits, long cacheMisses) {
            this.coldDurationMs = coldDurationMs;
            this.repeatedTotalMs = repeatedTotalMs;
            this.averageRepeatedMs = averageRepeatedMs;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
        }
    }
}
