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
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Engine Diagnostics Feature Tests")
class EngineDiagnosticsFeatureTest {

    @Test
    @DisplayName("Should expose execution statistics and diagnostics")
    void shouldExposeExecutionStatisticsAndDiagnostics() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(2000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .build())
                        .build()
        );

        try {
            assertTrue(executor.execute("return 1 + 1", "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());
            assertTrue(executor.execute("return 2 + 2", "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());

            EngineStatistics statistics = executor.getStatistics();
            EngineDiagnostics diagnostics = executor.getDiagnostics();

            assertEquals(2L, statistics.getTotalExecutions());
            assertEquals(2L, statistics.getSuccessfulExecutions());
            assertEquals(0L, statistics.getFailedExecutions());
            assertEquals(2L, statistics.getExecutionsByLanguage().get("groovy").longValue());

            assertNotNull(diagnostics);
            assertNotNull(diagnostics.getStatistics());
            assertNotNull(diagnostics.getExternalProcessStatistics());
            assertEquals("EXTERNAL_PROCESS", diagnostics.getIsolationMode());
            assertEquals("1", diagnostics.getExternalProcessStatistics().getWorkerProtocolVersion());
            assertTrue(diagnostics.getExternalProcessStatistics().getSupportedTransportProtocolCapabilities().contains("JSON_FRAMED"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose request validation diagnostics without borrowing a worker")
    void shouldExposeRequestValidationDiagnosticsWithoutBorrowingAWorker() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(2000)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .externalProcessPrewarmCount(0)
                                .build())
                        .build()
        );

        try {
            ScriptResult result = executor.execute(
                    "return value",
                    "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", new Object()))
            );

            assertEquals("request_validation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("request_payload_not_json_safe", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));

            EngineDiagnostics diagnostics = executor.getDiagnostics();
            assertNotNull(diagnostics.getExternalProcessStatistics());
            assertEquals(0, diagnostics.getExternalProcessStatistics().getCreatedWorkers());
            assertEquals(0L, diagnostics.getExternalProcessStatistics().getBorrowCount());
            assertEquals(1L, diagnostics.getExternalProcessStatistics().getFailureReasonCounts().get("request_payload_not_json_safe"));
            assertNull(diagnostics.getExternalProcessStatistics().getLatestBorrowFailureReason());
            assertNull(diagnostics.getExternalProcessStatistics().getLatestWorkerExecutionFailureReason());
        } finally {
            executor.close();
        }
    }
}
