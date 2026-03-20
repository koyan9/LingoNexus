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
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Isolation Tests")
class ExternalProcessIsolationTest {

    @Test
    @DisplayName("Should execute simple Groovy script in external process mode")
    void shouldExecuteSimpleGroovyScriptInExternalProcessMode() {
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(3000)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .build())
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        try {
            ScriptResult result = executor.execute(
                    "return value * 2",
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("value", 21))
            );

            assertTrue(result.isSuccess());
            assertEquals(42, result.getValue());
            assertEquals("EXTERNAL_PROCESS", result.getMetadata().get(ResultMetadataKeys.ISOLATION_MODE));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should reuse worker-local executor cache for repeated script")
    void shouldReuseWorkerLocalExecutorCacheForRepeatedScript() {
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(3000)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .externalProcessExecutorCacheMaxSize(4)
                        .externalProcessExecutorCacheIdleTtlMs(300000L)
                        .build())
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        try {
            ScriptResult result1 = executor.execute(
                    "return value + 1",
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("value", 41))
            );
            ScriptResult result2 = executor.execute(
                    "return value + 1",
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("value", 41))
            );

            assertTrue(result1.isSuccess());
            assertTrue(result2.isSuccess());
            assertEquals(42, ((Number) result2.getValue()).intValue());
            assertEquals(Boolean.TRUE, result2.getMetadata().get(ResultMetadataKeys.CACHE_HIT));

            EngineDiagnostics diagnostics = executor.getDiagnostics();
            assertTrue(diagnostics.getExternalProcessStatistics().getExecutorCacheHits() >= 1);
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should replace worker after timeout and recover on next execution")
    void shouldReplaceWorkerAfterTimeoutAndRecoverOnNextExecution() {
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(2000)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .build())
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        try {
            ScriptResult timeoutResult = executor.execute(
                    "Thread.sleep(4000); return 1",
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            );
            assertEquals(ExecutionStatus.TIMEOUT, timeoutResult.getStatus());
            assertEquals("worker_execution_timeout", timeoutResult.getMetadata().get(ResultMetadataKeys.ERROR_REASON));

            ScriptResult successResult = executor.execute(
                    "return 7 * 6",
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            );
            assertTrue(successResult.isSuccess());
            assertEquals(42, ((Number) successResult.getValue()).intValue());

            EngineDiagnostics diagnostics = executor.getDiagnostics();
            assertNotNull(diagnostics.getExternalProcessStatistics());
            assertTrue(diagnostics.getExternalProcessStatistics().getDiscardCount() >= 1);
            assertEquals("worker_execution_timeout", diagnostics.getExternalProcessStatistics().getLatestWorkerExecutionFailureReason());
            assertEquals(1L, diagnostics.getExternalProcessStatistics().getFailureReasonCounts().get("worker_execution_timeout"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose borrow timeout diagnostics when the worker pool is saturated")
    void shouldExposeBorrowTimeoutDiagnosticsWhenWorkerPoolIsSaturated() throws Exception {
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(3000)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .externalProcessPoolSize(1)
                        .externalProcessBorrowTimeoutMs(100L)
                        .build())
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        try {
            CompletableFuture<ScriptResult> blockingFuture = executor.executeAsync(
                    "Thread.sleep(1200); return 1",
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            );
            waitForBorrowToStart(executor, 2000L);

            ScriptResult borrowTimeoutResult = executor.execute(
                    "return 7 * 6",
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            );

            assertEquals(ExecutionStatus.FAILURE, borrowTimeoutResult.getStatus());
            assertEquals("borrow_worker", borrowTimeoutResult.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-pool", borrowTimeoutResult.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_borrow_timeout", borrowTimeoutResult.getMetadata().get(ResultMetadataKeys.ERROR_REASON));

            ScriptResult blockingResult = blockingFuture.get(5, TimeUnit.SECONDS);
            assertTrue(blockingResult.isSuccess());

            EngineDiagnostics diagnostics = executor.getDiagnostics();
            assertNotNull(diagnostics.getExternalProcessStatistics());
            assertTrue(diagnostics.getExternalProcessStatistics().getBorrowTimeoutCount() >= 1);
            assertEquals("worker_borrow_timeout", diagnostics.getExternalProcessStatistics().getLatestBorrowFailureReason());
            assertEquals(1L, diagnostics.getExternalProcessStatistics().getFailureReasonCounts().get("worker_borrow_timeout"));
        } finally {
            executor.close();
        }
    }

    private void waitForBorrowToStart(LingoNexusExecutor executor, long timeoutMs) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            EngineDiagnostics diagnostics = executor.getDiagnostics();
            if (diagnostics.getExternalProcessStatistics() != null
                    && diagnostics.getExternalProcessStatistics().getBorrowCount()
                    > diagnostics.getExternalProcessStatistics().getReturnCount()
                    && diagnostics.getExternalProcessStatistics().getIdleWorkers() == 0) {
                return;
            }
            Thread.sleep(25L);
        }
        throw new AssertionError("Timed out waiting for the first external worker borrow");
    }
}
