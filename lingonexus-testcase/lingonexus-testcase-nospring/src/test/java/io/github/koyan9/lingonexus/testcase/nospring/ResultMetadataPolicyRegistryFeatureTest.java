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
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistry;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyTemplate;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Result Metadata Policy Registry Feature Tests")
class ResultMetadataPolicyRegistryFeatureTest {

    @Test
    @DisplayName("Should share registry templates across multiple executors")
    void shouldShareRegistryTemplatesAcrossMultipleExecutors() {
        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistry.create()
                .registerTemplate(
                        ResultMetadataPolicyTemplate.builder()
                                .name("shared-thread-timing")
                                .parentPolicyName("timing")
                                .category(ResultMetadataCategory.THREAD)
                                .build()
                );

        LingoNexusExecutor directExecutor = createExecutor(ExecutionIsolationMode.DIRECT, registry);
        LingoNexusExecutor externalExecutor = createExecutor(ExecutionIsolationMode.EXTERNAL_PROCESS, registry);
        try {
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .putMetadata(MetadataKeys.RESULT_METADATA_POLICY, "shared-thread-timing")
                    .build();

            ScriptResult directResult = directExecutor.execute("return value * 2", "groovy", context);
            ScriptResult externalResult = externalExecutor.execute("return value * 2", "groovy", context);

            assertNotNull(directResult.getMetadata().get(ResultMetadataKeys.TOTAL_TIME));
            assertNotNull(directResult.getMetadata().get(ResultMetadataKeys.THREAD_NAME));
            assertNotNull(externalResult.getMetadata().get(ResultMetadataKeys.TOTAL_TIME));
            assertNotNull(externalResult.getMetadata().get(ResultMetadataKeys.THREAD_NAME));
        } finally {
            directExecutor.close();
            externalExecutor.close();
        }
    }

    @Test
    @DisplayName("Should let local templates override registry templates")
    void shouldLetLocalTemplatesOverrideRegistryTemplates() {
        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistry.create()
                .registerTemplate(
                        ResultMetadataPolicyTemplate.builder()
                                .name("shared-policy")
                                .parentPolicyName("timing")
                                .build()
                );

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .resultMetadataPolicyRegistry(registry)
                        .resultMetadataPolicyTemplate(
                                ResultMetadataPolicyTemplate.builder()
                                        .name("shared-policy")
                                        .category(ResultMetadataCategory.THREAD)
                                        .build()
                        )
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(3000L)
                                .isolationMode(ExecutionIsolationMode.DIRECT)
                                .build())
                        .build()
        );
        try {
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .putMetadata(MetadataKeys.RESULT_METADATA_POLICY, "shared-policy")
                    .build();

            Map<String, Object> metadata = executor.execute("return value * 2", "groovy", context).getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.TOTAL_TIME));
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode,
                                              ResultMetadataPolicyRegistry registry) {
        return LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .resultMetadataPolicyRegistry(registry)
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
