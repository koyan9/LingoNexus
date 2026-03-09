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
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Sandbox Provider Priority Feature Tests")
class SandboxProviderPriorityFeatureTest {

    @Test
    @DisplayName("Should select highest-priority provider for direct execution")
    void shouldSelectHighestPriorityProviderForDirectExecution() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L);
        try {
            assertTrue(executor.getSupportedLanguages().contains("priority-test"));
            assertEquals("HIGH", executor.execute("return 'ignored'", "priority-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should select highest-priority provider for external execution")
    void shouldSelectHighestPriorityProviderForExternalExecution() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L);
        try {
            assertEquals("HIGH", executor.execute("return 'ignored'", "priority-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode, long timeoutMs) {
        return LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("priority-test")
                        .sandboxConfig(SandboxConfig.builder()
                                .enabled(true)
                                .timeoutMs(timeoutMs)
                                .isolationMode(isolationMode)
                                .externalProcessExecutorCacheMaxSize(4)
                                .externalProcessExecutorCacheIdleTtlMs(300000L)
                                .build())
                        .build()
        );
    }
}
