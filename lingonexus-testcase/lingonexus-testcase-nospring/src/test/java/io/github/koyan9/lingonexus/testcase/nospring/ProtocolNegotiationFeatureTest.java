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
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Protocol Negotiation Feature Tests")
class ProtocolNegotiationFeatureTest {

    @Test
    @DisplayName("Should fail before execute when worker protocol capability is not negotiated")
    void shouldFailBeforeExecuteWhenWorkerProtocolCapabilityIsNotNegotiated() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("negotiation-test")
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
            assertEquals("protocol_negotiation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-handshake", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("protocol_capability_mismatch", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertTrue(result.getErrorMessage().contains("required capabilities"));
            assertEquals(1L, executor.getDiagnostics().getExternalProcessStatistics().getProtocolNegotiationFailureCount());
            assertTrue(executor.getDiagnostics().getExternalProcessStatistics().getLatestProtocolNegotiationFailureReason().contains("required capabilities"));
            assertEquals(1L, executor.getDiagnostics().getExternalProcessStatistics().getFailureReasonCounts().get("protocol_capability_mismatch"));
        } finally {
            executor.close();
        }
    }

    @Test
    @DisplayName("Should fail before execute when serializer contract is not negotiated")
    void shouldFailBeforeExecuteWhenSerializerContractIsNotNegotiated() {
        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(
                LingoNexusConfig.builder()
                        .defaultLanguage(ScriptLanguage.GROOVY)
                        .allowedSandboxLanguage("negotiation-test")
                        .requireSandboxTransportProtocolCapability(SandboxTransportProtocolCapability.CUSTOM_SERIALIZER_CONTRACT)
                        .requireSandboxTransportSerializerContractId("artifact-tool-v2")
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
            assertEquals("protocol_negotiation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-handshake", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("serializer_contract_mismatch", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertTrue(result.getErrorMessage().contains("required serializer contracts"));
            assertEquals(1L, executor.getDiagnostics().getExternalProcessStatistics().getProtocolNegotiationFailureCount());
            assertTrue(executor.getDiagnostics().getExternalProcessStatistics().getLatestProtocolNegotiationFailureReason().contains("required serializer contracts"));
            assertEquals(1L, executor.getDiagnostics().getExternalProcessStatistics().getFailureReasonCounts().get("serializer_contract_mismatch"));
        } finally {
            executor.close();
        }
    }
}
