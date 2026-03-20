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
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.api.security.ValidationResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Failure Diagnostics Performance Baseline Tests")
class FailureDiagnosticsPerformanceBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(FailureDiagnosticsPerformanceBaselineTest.class);
    private static final int ITERATIONS = 20;
    private static final String SUCCESS_SCRIPT = "return value * 2";
    private static final String FAILURE_SCRIPT = "return 'blocked'";

    @Test
    @DisplayName("Should compare diagnostics-heavy failure path against success-path metadata output")
    void shouldCompareDiagnosticsHeavyFailurePathAgainstSuccessPathMetadataOutput() {
        ScriptContext context = ScriptContext.of(Collections.<String, Object>singletonMap("value", 21));

        BenchmarkSnapshot successFull = benchmarkSuccess(ResultMetadataProfile.FULL, SUCCESS_SCRIPT, context);
        BenchmarkSnapshot failureErrorOnly = benchmarkFailureWithCategories(
                FAILURE_SCRIPT,
                context,
                ResultMetadataCategory.ERROR_DIAGNOSTICS
        );
        BenchmarkSnapshot failureFull = benchmarkFailureWithProfile(ResultMetadataProfile.FULL, FAILURE_SCRIPT, context);

        logger.info(
                "Failure-diagnostics baseline: successFull={}ms, failureErrorOnly={}ms, failureFull={}ms repeated avg",
                String.format("%.2f", successFull.averageRepeatedMs),
                String.format("%.2f", failureErrorOnly.averageRepeatedMs),
                String.format("%.2f", failureFull.averageRepeatedMs)
        );
        BenchmarkReportSupport.emit(
                "failure-diagnostics-direct",
                "scriptLanguage", "groovy",
                "isolationMode", ExecutionIsolationMode.DIRECT.name(),
                "iterations", ITERATIONS,
                "successFullColdMs", successFull.coldDurationMs,
                "successFullRepeatedTotalMs", successFull.repeatedTotalMs,
                "successFullAvgMs", String.format("%.2f", successFull.averageRepeatedMs),
                "successFullCacheHits", successFull.cacheHits,
                "successFullCacheMisses", successFull.cacheMisses,
                "failureErrorOnlyColdMs", failureErrorOnly.coldDurationMs,
                "failureErrorOnlyRepeatedTotalMs", failureErrorOnly.repeatedTotalMs,
                "failureErrorOnlyAvgMs", String.format("%.2f", failureErrorOnly.averageRepeatedMs),
                "failureErrorOnlyCacheHits", failureErrorOnly.cacheHits,
                "failureErrorOnlyCacheMisses", failureErrorOnly.cacheMisses,
                "failureFullColdMs", failureFull.coldDurationMs,
                "failureFullRepeatedTotalMs", failureFull.repeatedTotalMs,
                "failureFullAvgMs", String.format("%.2f", failureFull.averageRepeatedMs),
                "failureFullCacheHits", failureFull.cacheHits,
                "failureFullCacheMisses", failureFull.cacheMisses
        );

        assertEquals(ITERATIONS, successFull.cacheHits);
        assertEquals(1L, successFull.cacheMisses);
        assertEquals(0L, failureErrorOnly.cacheHits);
        assertEquals(0L, failureErrorOnly.cacheMisses);
        assertEquals(0L, failureFull.cacheHits);
        assertEquals(0L, failureFull.cacheMisses);
    }

    private BenchmarkSnapshot benchmarkSuccess(ResultMetadataProfile profile, String script, ScriptContext context) {
        LingoNexusExecutor executor = createExecutor(profile, null);
        try {
            long coldStart = System.nanoTime();
            ScriptResult coldResult = executor.execute(script, "groovy", context);
            long coldDurationMs = nanosToMillis(System.nanoTime() - coldStart);

            assertTrue(coldResult.isSuccess());
            assertEquals(42, ((Number) coldResult.getValue()).intValue());
            assertSuccessFullMetadata(coldResult);

            long repeatedStart = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                ScriptResult result = executor.execute(script, "groovy", context);
                assertTrue(result.isSuccess());
                assertEquals(42, ((Number) result.getValue()).intValue());
                assertSuccessFullMetadata(result);
            }
            long repeatedDurationMs = nanosToMillis(System.nanoTime() - repeatedStart);

            return new BenchmarkSnapshot(
                    coldDurationMs,
                    repeatedDurationMs,
                    repeatedDurationMs / (double) ITERATIONS,
                    executor.getStatistics().getCacheHits(),
                    executor.getStatistics().getCacheMisses()
            );
        } finally {
            executor.close();
        }
    }

    private BenchmarkSnapshot benchmarkFailureWithProfile(ResultMetadataProfile profile, String script, ScriptContext context) {
        LingoNexusExecutor executor = createExecutor(profile, null);
        try {
            return runFailureBenchmark(executor, script, context, true);
        } finally {
            executor.close();
        }
    }

    private BenchmarkSnapshot benchmarkFailureWithCategories(String script, ScriptContext context,
                                                             ResultMetadataCategory... categories) {
        LingoNexusExecutor executor = createExecutor(null, categories);
        try {
            return runFailureBenchmark(executor, script, context, false);
        } finally {
            executor.close();
        }
    }

    private BenchmarkSnapshot runFailureBenchmark(LingoNexusExecutor executor, String script, ScriptContext context,
                                                  boolean expectTimingFields) {
        long coldStart = System.nanoTime();
        ScriptResult coldResult = executor.execute(script, "groovy", context);
        long coldDurationMs = nanosToMillis(System.nanoTime() - coldStart);

        assertFalse(coldResult.isSuccess());
        assertFailureMetadata(coldResult, expectTimingFields);

        long repeatedStart = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            ScriptResult result = executor.execute(script, "groovy", context);
            assertFalse(result.isSuccess());
            assertFailureMetadata(result, expectTimingFields);
        }
        long repeatedDurationMs = nanosToMillis(System.nanoTime() - repeatedStart);

        return new BenchmarkSnapshot(
                coldDurationMs,
                repeatedDurationMs,
                repeatedDurationMs / (double) ITERATIONS,
                executor.getStatistics().getCacheHits(),
                executor.getStatistics().getCacheMisses()
        );
    }

    private void assertSuccessFullMetadata(ScriptResult result) {
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
        assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
        assertNotNull(metadata.get(ResultMetadataKeys.SECURITY_CHECKS));
        assertNotNull(metadata.get(ResultMetadataKeys.MODULES_USED));
    }

    private void assertFailureMetadata(ScriptResult result, boolean expectTimingFields) {
        Map<String, Object> metadata = result.getMetadata();
        assertEquals("ScriptSecurityException", metadata.get(ResultMetadataKeys.ERROR_TYPE));
        assertEquals(ExecutionIsolationMode.DIRECT.name(), metadata.get(ResultMetadataKeys.ISOLATION_MODE));
        if (expectTimingFields) {
            assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
        } else {
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
        }
    }

    private LingoNexusExecutor createExecutor(ResultMetadataProfile profile,
                                              ResultMetadataCategory... categories) {
        LingoNexusConfig.Builder builder = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                .addSecurityPolicy(new BlockWordSecurityPolicy("blocked"))
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(3000L)
                        .isolationMode(ExecutionIsolationMode.DIRECT)
                        .build());
        if (profile != null) {
            builder.resultMetadataProfile(profile);
        }
        if (categories != null && categories.length > 0) {
            builder.resultMetadataCategory(categories);
        }
        return LingoNexusBuilder.createNewInstance(builder.build());
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

        private BenchmarkSnapshot(long coldDurationMs, long repeatedTotalMs, double averageRepeatedMs,
                                  long cacheHits, long cacheMisses) {
            this.coldDurationMs = coldDurationMs;
            this.repeatedTotalMs = repeatedTotalMs;
            this.averageRepeatedMs = averageRepeatedMs;
            this.cacheHits = cacheHits;
            this.cacheMisses = cacheMisses;
        }
    }

    private static final class BlockWordSecurityPolicy implements SecurityPolicy {
        private final String blockedWord;

        private BlockWordSecurityPolicy(String blockedWord) {
            this.blockedWord = blockedWord;
        }

        @Override
        public ValidationResult validate(String script, String language, ScriptContext context,
                                         io.github.koyan9.lingonexus.api.config.SandboxConfig config) {
            if (script != null && script.contains(blockedWord)) {
                return ValidationResult.failure("blocked keyword detected: " + blockedWord);
            }
            return ValidationResult.success();
        }

        @Override
        public String getName() {
            return "BlockWordSecurityPolicy";
        }
    }
}
