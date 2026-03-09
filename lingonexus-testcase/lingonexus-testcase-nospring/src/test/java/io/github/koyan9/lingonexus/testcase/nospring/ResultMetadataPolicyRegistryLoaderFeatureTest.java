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
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistry;
import io.github.koyan9.lingonexus.api.result.ResultMetadataPolicyRegistryLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Result Metadata Policy Registry Loader Feature Tests")
class ResultMetadataPolicyRegistryLoaderFeatureTest {

    @Test
    @DisplayName("Should build registry from properties and apply it to external execution")
    void shouldBuildRegistryFromPropertiesAndApplyItToExternalExecution() {
        Properties properties = new Properties();
        properties.setProperty("resultMetadataPolicies.teamTimingThread.parent", "timing");
        properties.setProperty("resultMetadataPolicies.teamTimingThread.categories", "thread");

        ResultMetadataPolicyRegistry registry = ResultMetadataPolicyRegistryLoader.load(properties);

        LingoNexusExecutor executor = io.github.koyan9.lingonexus.core.LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
                        .resultMetadataPolicyRegistry(registry)
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
            ScriptContext context = ScriptContext.builder()
                    .put("value", 21)
                    .putMetadata(MetadataKeys.RESULT_METADATA_POLICY, "teamTimingThread")
                    .build();

            Map<String, Object> metadata = executor.execute("return value * 2", "groovy", context).getMetadata();
            assertNotNull(metadata.get(ResultMetadataKeys.TOTAL_TIME));
            assertNotNull(metadata.get(ResultMetadataKeys.THREAD_NAME));
            assertFalse(metadata.containsKey(ResultMetadataKeys.MODULES_USED));
        } finally {
            executor.close();
        }
    }
}
