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
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
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

@DisplayName("Result Metadata Profile Performance Baseline Tests")
class ResultMetadataProfilePerformanceBaselineTest {

    private static final Logger logger = LoggerFactory.getLogger(ResultMetadataProfilePerformanceBaselineTest.class);
    private static final int ITERATIONS = 20;

    @Test
    @DisplayName("Should establish direct execution baseline across result metadata profiles")
    void shouldEstablishDirectExecutionBaselineAcrossResultMetadataProfiles() {
        ScriptContext context = ScriptContext.of(Collections.<String, Object>singletonMap("value", 21));
        String script = "return benchmarkModule != null ? value * 2 : 0";

        BenchmarkSnapshot basic = benchmark(ResultMetadataProfile.BASIC, script, context);
        BenchmarkSnapshot timing = benchmark(ResultMetadataProfile.TIMING, script, context);
        BenchmarkSnapshot full = benchmark(ResultMetadataProfile.FULL, script, context);

        logger.info(
                "Metadata-profile baseline: basic={}ms, timing={}ms, full={}ms repeated avg (cold basic={}ms, timing={}ms, full={}ms)",
                String.format("%.2f", basic.averageRepeatedMs),
                String.format("%.2f", timing.averageRepeatedMs),
                String.format("%.2f", full.averageRepeatedMs),
                basic.coldDurationMs,
                timing.coldDurationMs,
                full.coldDurationMs
        );
        BenchmarkReportSupport.emit(
                "metadata-profile-direct",
                "scriptLanguage", "groovy",
                "isolationMode", ExecutionIsolationMode.DIRECT.name(),
                "iterations", ITERATIONS,
                "basicColdMs", basic.coldDurationMs,
                "basicRepeatedTotalMs", basic.repeatedTotalMs,
                "basicAvgMs", String.format("%.2f", basic.averageRepeatedMs),
                "basicCacheHits", basic.cacheHits,
                "basicCacheMisses", basic.cacheMisses,
                "timingColdMs", timing.coldDurationMs,
                "timingRepeatedTotalMs", timing.repeatedTotalMs,
                "timingAvgMs", String.format("%.2f", timing.averageRepeatedMs),
                "timingCacheHits", timing.cacheHits,
                "timingCacheMisses", timing.cacheMisses,
                "fullColdMs", full.coldDurationMs,
                "fullRepeatedTotalMs", full.repeatedTotalMs,
                "fullAvgMs", String.format("%.2f", full.averageRepeatedMs),
                "fullCacheHits", full.cacheHits,
                "fullCacheMisses", full.cacheMisses
        );

        assertEquals(ITERATIONS, basic.cacheHits);
        assertEquals(ITERATIONS, timing.cacheHits);
        assertEquals(ITERATIONS, full.cacheHits);
        assertEquals(1L, basic.cacheMisses);
        assertEquals(1L, timing.cacheMisses);
        assertEquals(1L, full.cacheMisses);
    }

    private BenchmarkSnapshot benchmark(ResultMetadataProfile profile, String script, ScriptContext context) {
        LingoNexusExecutor executor = createExecutor(profile);
        try {
            long coldStart = System.nanoTime();
            ScriptResult coldResult = executor.execute(script, "groovy", context);
            long coldDurationMs = nanosToMillis(System.nanoTime() - coldStart);

            assertTrue(coldResult.isSuccess());
            assertEquals(42, ((Number) coldResult.getValue()).intValue());
            assertMetadataMatchesProfile(coldResult, profile);

            long repeatedStart = System.nanoTime();
            for (int i = 0; i < ITERATIONS; i++) {
                ScriptResult result = executor.execute(script, "groovy", context);
                assertTrue(result.isSuccess());
                assertEquals(42, ((Number) result.getValue()).intValue());
                assertMetadataMatchesProfile(result, profile);
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

    private void assertMetadataMatchesProfile(ScriptResult result, ResultMetadataProfile profile) {
        Map<String, Object> metadata = result.getMetadata();
        assertNotNull(metadata.get(ResultMetadataKeys.SCRIPT_ENGINE));
        assertNotNull(metadata.get(ResultMetadataKeys.ISOLATION_MODE));
        switch (profile) {
            case BASIC:
                assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
                assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
                assertFalse(metadata.containsKey(ResultMetadataKeys.MODULES_USED));
                assertFalse(metadata.containsKey(ResultMetadataKeys.SECURITY_CHECKS));
                break;
            case TIMING:
                assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
                assertNotNull(metadata.get(ResultMetadataKeys.COMPILE_TIME));
                assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
                assertFalse(metadata.containsKey(ResultMetadataKeys.MODULES_USED));
                assertFalse(metadata.containsKey(ResultMetadataKeys.SECURITY_CHECKS));
                break;
            case FULL:
                assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
                assertNotNull(metadata.get(ResultMetadataKeys.COMPILE_TIME));
                assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
                assertNotNull(metadata.get(ResultMetadataKeys.MODULES_USED));
                assertNotNull(metadata.get(ResultMetadataKeys.SECURITY_CHECKS));
                break;
            default:
                throw new IllegalStateException("Unhandled profile: " + profile);
        }
    }

    private LingoNexusExecutor createExecutor(ResultMetadataProfile profile) {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .resultMetadataProfile(profile)
                        .addSecurityPolicy(new AllowAllSecurityPolicy())
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000L)
                                .isolationMode(ExecutionIsolationMode.DIRECT)
                                .build())
                        .build()
        );
        executor.registerModule(new BenchmarkModule("benchmarkModule"));
        return executor;
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

    private static final class AllowAllSecurityPolicy implements SecurityPolicy {

        @Override
        public ValidationResult validate(String script, String language, ScriptContext context,
                                         io.github.koyan9.lingonexus.api.config.SandboxConfig config) {
            return ValidationResult.success();
        }

        @Override
        public String getName() {
            return "AllowAllSecurityPolicy";
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
