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
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Sandbox Provider Capability Feature Tests")
class SandboxProviderCapabilityFeatureTest {

    @Test
    @DisplayName("Should keep highest-priority provider when no capability filter is set")
    void shouldKeepHighestPriorityProviderWhenNoCapabilityFilterIsSet() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L).build());
        try {
            assertEquals("POLYGLOT", executor.execute("return 'ignored'", "capability-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should select engine-cache-capable provider when required")
    void shouldSelectEngineCacheCapableProviderWhenRequired() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .requireEngineCacheCapableSandbox(true)
                        .build()
        );
        try {
            assertEquals("CACHE_CAPABLE", executor.execute("return 'ignored'", "capability-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should select host-access-compatible provider when restricted")
    void shouldSelectHostAccessCompatibleProviderWhenRestricted() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .allowedSandboxHostAccessMode(SandboxHostAccessMode.JVM_CLASSLOADER)
                        .build()
        );
        try {
            assertEquals("CACHE_CAPABLE", executor.execute("return 'ignored'", "capability-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require external-process-compatible provider for external isolation")
    void shouldRequireExternalProcessCompatibleProviderForExternalIsolation() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(createBaseBuilder(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L).build());
        try {
            assertEquals("CACHE_CAPABLE", executor.execute("return 'ignored'", "capability-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    private LingoNexusConfig.Builder createBaseBuilder(ExecutionIsolationMode isolationMode, long timeoutMs) {
        return LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .allowedSandboxLanguage("capability-test")
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(timeoutMs)
                        .isolationMode(isolationMode)
                        .externalProcessExecutorCacheMaxSize(4)
                        .externalProcessExecutorCacheIdleTtlMs(300000L)
                        .build());
    }
}
