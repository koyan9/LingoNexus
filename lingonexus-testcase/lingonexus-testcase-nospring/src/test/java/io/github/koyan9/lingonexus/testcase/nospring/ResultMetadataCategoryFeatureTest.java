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
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.result.ResultMetadataProfile;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Result Metadata Category Feature Tests")
class ResultMetadataCategoryFeatureTest {

    @Test
    @DisplayName("Should emit only configured timing and thread categories in direct mode")
    void shouldEmitOnlyConfiguredTimingAndThreadCategoriesInDirectMode() {
        LingoNexusExecutor executor = createExecutor(
                ExecutionIsolationMode.DIRECT,
                ScriptLanguage.GROOVY.getId(),
                ResultMetadataProfile.BASIC,
                ResultMetadataCategory.TIMING,
                ResultMetadataCategory.THREAD
        );
        try {
            ScriptResult result = executor.execute("return value * 2", "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 21)));

            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.COMPILE_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.MODULES_USED));
            assertFalse(metadata.containsKey(ResultMetadataKeys.SECURITY_CHECKS));
        } finally {
            executor.close();
        }
    }


    @Test
    @DisplayName("Should fall back to engine categories when request metadata is empty")
    void shouldFallBackToEngineCategoriesWhenRequestMetadataIsEmpty() {
        LingoNexusExecutor executor = createExecutor(
                ExecutionIsolationMode.DIRECT,
                ScriptLanguage.GROOVY.getId(),
                ResultMetadataProfile.BASIC,
                ResultMetadataCategory.MODULE
        );
        try {
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .build();

            ScriptResult result = executor.execute("return value * 2", "groovy", context);
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.MODULES_USED));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should allow request category override from comma-separated string")
    void shouldAllowRequestCategoryOverrideFromCommaSeparatedString() {
        LingoNexusExecutor executor = createExecutor(
                ExecutionIsolationMode.DIRECT,
                ScriptLanguage.GROOVY.getId(),
                ResultMetadataProfile.BASIC
        );
        try {
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .putMetadata(MetadataKeys.RESULT_METADATA_CATEGORIES, "thread,module")
                    .build();

            ScriptResult result = executor.execute("return value * 2", "groovy", context);
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
            assertNotNull(metadata.get(ResultMetadataKeys.MODULES_USED));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
        } finally {
            executor.close();
        }
    }


    @Test
    @DisplayName("Should propagate thread category through external mode")
    void shouldPropagateThreadCategoryThroughExternalMode() {
        LingoNexusExecutor executor = createExecutor(
                ExecutionIsolationMode.EXTERNAL_PROCESS,
                ScriptLanguage.GROOVY.getId(),
                ResultMetadataProfile.BASIC,
                ResultMetadataCategory.THREAD
        );
        try {
            ScriptResult result = executor.execute("return value * 2", "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 21)));
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_ID));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.MODULES_USED));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should propagate module category through external mode")
    void shouldPropagateModuleCategoryThroughExternalMode() {
        LingoNexusExecutor executor = createExecutor(
                ExecutionIsolationMode.EXTERNAL_PROCESS,
                ScriptLanguage.GROOVY.getId(),
                ResultMetadataProfile.BASIC,
                ResultMetadataCategory.MODULE
        );
        try {
            ScriptResult result = executor.execute("return value * 2", "groovy",
                    ScriptContext.of(Collections.<String, Object>singletonMap("value", 21)));
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.MODULES_USED));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should expose only error diagnostics category for negotiation failure")
    void shouldExposeOnlyErrorDiagnosticsCategoryForNegotiationFailure() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("negotiation-test")
                        .resultMetadataCategory(ResultMetadataCategory.ERROR_DIAGNOSTICS)
                        .requireSandboxTransportProtocolCapability(SandboxTransportProtocolCapability.CBOR_CAPABLE)
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000L)
                                .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                                .externalProcessExecutorCacheMaxSize(4)
                                .externalProcessExecutorCacheIdleTtlMs(300000L)
                                .build())
                        .build()
        );
        try {
            ScriptResult result = executor.execute("return 'ignored'", "negotiation-test", ScriptContext.of(Collections.emptyMap()));
            Map<String, Object> metadata = result.getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.ERROR_STAGE));
            assertNotNull(metadata.get(ResultMetadataKeys.ERROR_COMPONENT));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.THREAD_NAME));
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode,
                                              String language,
                                              ResultMetadataProfile resultMetadataProfile,
                                              ResultMetadataCategory... categories) {
        LingoNexusConfig.Builder builder = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .allowedSandboxLanguage(language)
                .resultMetadataProfile(resultMetadataProfile)
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(3000L)
                        .isolationMode(isolationMode)
                        .externalProcessExecutorCacheMaxSize(4)
                        .externalProcessExecutorCacheIdleTtlMs(300000L)
                        .build());
        if (categories != null && categories.length > 0) {
            builder.resultMetadataCategory(categories);
        }
        return LingoNexusBuilder.createNewInstance(builder.build());
    }
}
