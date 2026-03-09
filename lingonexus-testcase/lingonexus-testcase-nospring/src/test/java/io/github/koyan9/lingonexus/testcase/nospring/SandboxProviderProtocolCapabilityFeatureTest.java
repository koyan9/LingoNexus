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
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Sandbox Provider Protocol Capability Feature Tests")
class SandboxProviderProtocolCapabilityFeatureTest {

    @Test
    @DisplayName("Should keep highest-priority provider when no protocol filter is set")
    void shouldKeepHighestPriorityProviderWhenNoProtocolFilterIsSet() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L);
        try {
            assertEquals("PROTOCOL_JSON", executor.execute("return 'ignored'", "protocol-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by CBOR capability")
    void shouldFilterByCborCapability() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L, SandboxTransportProtocolCapability.CBOR_CAPABLE);
        try {
            assertEquals("PROTOCOL_CBOR", executor.execute("return 'ignored'", "protocol-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by streaming capability")
    void shouldFilterByStreamingCapability() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.DIRECT, 0L, SandboxTransportProtocolCapability.STREAMING_CAPABLE);
        try {
            assertEquals("PROTOCOL_STREAMING", executor.execute("return 'ignored'", "protocol-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should filter by custom serializer contract capability")
    void shouldFilterByCustomSerializerContractCapability() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                createBaseBuilder(ExecutionIsolationMode.DIRECT, 0L)
                        .requireSandboxTransportProtocolCapability(SandboxTransportProtocolCapability.CUSTOM_SERIALIZER_CONTRACT)
                        .requireSandboxTransportSerializerContractId("artifact-tool-v1")
                        .build()
        );
        try {
            assertEquals("PROTOCOL_CONTRACT", executor.execute("return 'ignored'", "protocol-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should require JSON-framed provider for external process mode")
    void shouldRequireJsonFramedProviderForExternalProcessMode() {
        LingoNexusExecutor executor = createExecutor(ExecutionIsolationMode.EXTERNAL_PROCESS, 3000L);
        try {
            assertEquals("PROTOCOL_JSON", executor.execute("return 'ignored'", "protocol-test", ScriptContext.of(Collections.emptyMap())).getValue());
        } finally {
            executor.close();
        }
    }

    private LingoNexusExecutor createExecutor(ExecutionIsolationMode isolationMode, long timeoutMs,
                                              SandboxTransportProtocolCapability... protocolCapabilities) {
        LingoNexusConfig.Builder builder = createBaseBuilder(isolationMode, timeoutMs);
        if (protocolCapabilities != null && protocolCapabilities.length > 0) {
            builder.requireSandboxTransportProtocolCapability(protocolCapabilities);
        }
        return LingoNexusBuilder.createNewInstance(builder.build());
    }

    private LingoNexusConfig.Builder createBaseBuilder(ExecutionIsolationMode isolationMode, long timeoutMs) {
        return LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .allowedSandboxLanguage("protocol-test")
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(timeoutMs)
                        .isolationMode(isolationMode)
                        .externalProcessExecutorCacheMaxSize(4)
                        .externalProcessExecutorCacheIdleTtlMs(300000L)
                        .build());
    }
}
