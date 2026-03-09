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
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Sandbox Provider Restriction Flag Feature Tests")
class SandboxProviderRestrictionFlagFeatureTest {

    @Test
    @DisplayName("Should keep highest-priority provider when no restriction flags are required")
    void shouldKeepHighestPriorityProviderWhenNoRestrictionFlagsAreRequired() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L);
        try {
            assertEquals("NO_FLAGS", executor.execute("return 'ignored'", "restriction-flag-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require reflection-blocked provider")
    void shouldRequireReflectionBlockedProvider() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L, SandboxHostRestrictionFlag.REFLECTION_BLOCKED);
        try {
            assertEquals("REFLECTION_FILE", executor.execute("return 'ignored'", "restriction-flag-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require reflection and file-blocked provider")
    void shouldRequireReflectionAndFileBlockedProvider() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L,
                SandboxHostRestrictionFlag.REFLECTION_BLOCKED,
                SandboxHostRestrictionFlag.FILE_IO_BLOCKED);
        try {
            assertEquals("REFLECTION_FILE", executor.execute("return 'ignored'", "restriction-flag-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require network-blocked provider")
    void shouldRequireNetworkBlockedProvider() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L, SandboxHostRestrictionFlag.NETWORK_BLOCKED);
        try {
            assertEquals("ALL_FLAGS", executor.execute("return 'ignored'", "restriction-flag-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require restriction flags in external process mode")
    void shouldRequireRestrictionFlagsInExternalProcessMode() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L,
                SandboxHostRestrictionFlag.REFLECTION_BLOCKED,
                SandboxHostRestrictionFlag.FILE_IO_BLOCKED,
                SandboxHostRestrictionFlag.NETWORK_BLOCKED);
        try {
            assertEquals("ALL_FLAGS", executor.execute("return 'ignored'", "restriction-flag-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode, long timeoutMs,
                                              SandboxHostRestrictionFlag... flags) {
        return LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("restriction-flag-test")
                        .requireSandboxHostRestrictionFlag(flags)
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
