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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Selective Sandbox Loading Feature Tests")
class SelectiveSandboxLoadingFeatureTest {

    @Test
    @DisplayName("Should load only Groovy sandbox for direct execution")
    void shouldLoadOnlyGroovySandboxForDirectExecution() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L);
        try {
            assertTrue(executor.execute("return 1 + 2", "groovy", ScriptContext.of(Collections.emptyMap())).isSuccess());
            assertTrue(executor.getSupportedLanguages().contains("groovy"));
            assertFalse(executor.getSupportedLanguages().contains("javascript"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should execute external process with Groovy-only sandbox loading")
    void shouldExecuteExternalProcessWithGroovyOnlySandboxLoading() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L);
        try {
            assertEquals(3, ((Number) executor.execute("return 1 + 2", "groovy", ScriptContext.of(Collections.emptyMap())).getValue()).intValue());
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode, long timeoutMs) {
        return LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage(ScriptLanguage.GROOVY)
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
