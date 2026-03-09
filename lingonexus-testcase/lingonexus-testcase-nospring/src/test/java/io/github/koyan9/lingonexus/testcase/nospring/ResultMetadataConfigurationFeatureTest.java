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
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.MetadataKeys;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Result Metadata Configuration Feature Tests")
class ResultMetadataConfigurationFeatureTest {

    @Test
    @DisplayName("Should disable detailed metadata by engine config in direct mode")
    void shouldDisableDetailedMetadataByEngineConfigInDirectMode() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, false);
        try {
            ScriptResult result = executor.execute("return value * 2", "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 21)));

            assertTrue(result.isSuccess());
            Map<String, Object> metadata = result.getMetadata();
            assertEquals("groovy", metadata.get(ResultMetadataKeys.SCRIPT_ENGINE));
            assertEquals(ExecutionIsolationMode.DIRECT.name(), metadata.get(ResultMetadataKeys.ISOLATION_MODE));
            assertNotNull(metadata.get(ResultMetadataKeys.CACHE_HIT));
            assertFalse(metadata.containsKey(ResultMetadataKeys.COMPILE_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.MODULES_USED));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should allow request metadata to enable detailed metadata")
    void shouldAllowRequestMetadataToEnableDetailedMetadata() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, false);
        try {
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .putMetadata(MetadataKeys.DETAILED_RESULT_METADATA_ENABLED, true)
                    .build();

            ScriptResult result = executor.execute("return value * 2", "groovy", context);

            assertTrue(result.isSuccess());
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.COMPILE_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should allow request metadata to disable detailed metadata")
    void shouldAllowRequestMetadataToDisableDetailedMetadata() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, true);
        try {
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .putMetadata(MetadataKeys.DETAILED_RESULT_METADATA_ENABLED, false)
                    .build();

            ScriptResult result = executor.execute("return value * 2", "groovy", context);

            assertTrue(result.isSuccess());
            Map<String, Object> metadata = result.getMetadata();
            assertEquals("groovy", metadata.get(ResultMetadataKeys.SCRIPT_ENGINE));
            assertFalse(metadata.containsKey(ResultMetadataKeys.COMPILE_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should disable detailed metadata by engine config in external mode")
    void shouldDisableDetailedMetadataByEngineConfigInExternalMode() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.EXTERNAL_PROCESS, false);
        try {
            ScriptResult result = executor.execute("return value * 2", "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 21)));

            assertTrue(result.isSuccess());
            assertEquals(42, ((Number) result.getValue()).intValue());

            Map<String, Object> metadata = result.getMetadata();
            assertEquals("groovy", metadata.get(ResultMetadataKeys.SCRIPT_ENGINE));
            assertEquals(ExecutionIsolationMode.EXTERNAL_PROCESS.name(), metadata.get(ResultMetadataKeys.ISOLATION_MODE));
            assertNotNull(metadata.get(ResultMetadataKeys.CACHE_HIT));
            assertFalse(metadata.containsKey(ResultMetadataKeys.COMPILE_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.ERROR_STAGE));
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode,
                                              boolean detailedResultMetadataEnabled) {
        return LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .detailedResultMetadataEnabled(detailedResultMetadataEnabled)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000L)
                                .isolationMode(isolationMode)
                                .externalProcessExecutorCacheMaxSize(4)
                                .externalProcessExecutorCacheIdleTtlMs(300000L)
                                .build())
                        .build()
        );
    }
}
