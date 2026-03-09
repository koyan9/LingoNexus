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
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Sandbox Provider Transport Capability Feature Tests")
class SandboxProviderTransportCapabilityFeatureTest {

    @Test
    @DisplayName("Should keep highest-priority provider when no transport filter is set")
    void shouldKeepHighestPriorityProviderWhenNoTransportFilterIsSet() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L).build());
        try {
            assertEquals("ANY", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require JSON-safe external result provider")
    void shouldRequireJsonSafeExternalResultProvider() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .requireJsonSafeExternalResult(true)
                        .build()
        );
        try {
            assertEquals("JSON_RESULT", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require JSON-safe external metadata provider")
    void shouldRequireJsonSafeExternalMetadataProvider() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .requireJsonSafeExternalMetadata(true)
                        .build()
        );
        try {
            assertEquals("JSON_RESULT_AND_METADATA", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by result transport mode explicitly")
    void shouldFilterByResultTransportModeExplicitly() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .allowedSandboxResultTransportMode(SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA)
                        .build()
        );
        try {
            assertEquals("JSON_RESULT_AND_METADATA", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }


    @Test
    @DisplayName("Should filter by transport serializer mode explicitly")
    void shouldFilterByTransportSerializerModeExplicitly() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .allowedSandboxTransportSerializerMode(SandboxTransportSerializerMode.BINARY_FRIENDLY)
                        .build()
        );
        try {
            assertEquals("BINARY_FRIENDLY", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by custom serializer requirement explicitly")
    void shouldFilterByCustomSerializerRequirementExplicitly() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .allowedSandboxTransportSerializerMode(SandboxTransportSerializerMode.CUSTOM_SERIALIZER_REQUIRED)
                        .build()
        );
        try {
            assertEquals("CUSTOM_SERIALIZER", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by large payload profile explicitly")
    void shouldFilterByLargePayloadProfileExplicitly() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .allowedSandboxTransportPayloadProfile(SandboxTransportPayloadProfile.LARGE_PAYLOAD_FRIENDLY)
                        .allowedSandboxTransportSerializerMode(SandboxTransportSerializerMode.JSON_FRAMED)
                        .build()
        );
        try {
            assertEquals("JSON_RESULT_AND_METADATA", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by host restriction mode explicitly")
    void shouldFilterByHostRestrictionModeExplicitly() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .allowedSandboxHostRestrictionMode(SandboxHostRestrictionMode.STRICT)
                        .allowedSandboxResultTransportMode(SandboxResultTransportMode.JSON_SAFE_RESULT_AND_METADATA)
                        .build()
        );
        try {
            assertEquals("JSON_RESULT_AND_METADATA", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require transport-compatible provider for external isolation")
    void shouldRequireTransportCompatibleProviderForExternalIsolation() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(createBaseBuilder(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L).build());
        try {
            assertEquals("JSON_RESULT", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require metadata-safe provider when external metadata safety is requested")
    void shouldRequireMetadataSafeProviderWhenExternalMetadataSafetyIsRequested() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L)
                        .requireJsonSafeExternalMetadata(true)
                        .build()
        );
        try {
            assertEquals("JSON_RESULT_AND_METADATA", executor.execute("return 'ignored'", "transport-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    private LingoNexusConfig.Builder createBaseBuilder(ExecutionIsolationMode isolationMode, long timeoutMs) {
        return LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .allowedSandboxLanguage("transport-test")
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(timeoutMs)
                        .isolationMode(isolationMode)
                        .externalProcessExecutorCacheMaxSize(4)
                        .externalProcessExecutorCacheIdleTtlMs(300000L)
                        .build());
    }
}
