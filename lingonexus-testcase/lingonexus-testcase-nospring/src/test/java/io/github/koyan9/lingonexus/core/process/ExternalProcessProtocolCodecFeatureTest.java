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
package io.github.koyan9.lingonexus.core.process;

import io.github.koyan9.lingonexus.api.config.CacheConfig;
import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.result.ResultMetadataCategory;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import io.github.koyan9.lingonexus.core.impl.DefaultModuleRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Protocol Codec Feature Tests")
class ExternalProcessProtocolCodecFeatureTest {

    @Test
    @DisplayName("Should round-trip external process request payload")
    void shouldRoundTripExternalProcessRequestPayload() throws Exception {
        EngineConfig config = EngineConfig.builder()
                .defaultLanguage("groovy")
                .cacheConfig(CacheConfig.builder().enabled(true).maxSize(128).build())
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(1000L)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .build())
                .requiredSandboxTransportProtocolCapabilities(
                        EnumSet.of(SandboxTransportProtocolCapability.JSON_FRAMED)
                )
                .resultMetadataCategory(ResultMetadataCategory.MODULE, ResultMetadataCategory.THREAD)
                .build();
        ExternalProcessExecutionRequestFactory factory = new ExternalProcessExecutionRequestFactory(
                config,
                new DefaultModuleRegistry(),
                Collections.emptyList()
        );

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("status", SampleState.READY);
        variables.put("numbers", new int[]{1, 2, 3});

        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("trace", Arrays.asList("a", "b"));

        ExternalProcessExecutionRequest request = factory.createRequest(
                "return numbers[0]",
                "groovy",
                ScriptContext.of(variables, metadata),
                variables
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ExternalProcessProtocolCodec.writeRequest(outputStream, request);
        ExternalProcessExecutionRequest decoded = ExternalProcessProtocolCodec.readRequest(
                new ByteArrayInputStream(outputStream.toByteArray())
        );

        assertEquals(request.getRequestType(), decoded.getRequestType());
        assertEquals(request.getLanguage(), decoded.getLanguage());
        assertEquals(request.getDefaultLanguage(), decoded.getDefaultLanguage());
        assertEquals(request.getAllowedSandboxLanguages(), decoded.getAllowedSandboxLanguages());
        assertEquals(request.getResultMetadataCategories(), decoded.getResultMetadataCategories());
        assertEquals(request.getRequiredSandboxTransportProtocolCapabilities(), decoded.getRequiredSandboxTransportProtocolCapabilities());
        assertEquals(request.getVariables(), decoded.getVariables());
        assertEquals(request.getMetadata(), decoded.getMetadata());
    }


    @Test
    @DisplayName("Should reuse health-check request and protocol capability snapshots")
    void shouldReuseHealthCheckRequestAndProtocolCapabilitySnapshots() {
        assertSame(ExternalProcessExecutionRequest.healthCheck(), ExternalProcessExecutionRequest.healthCheck());
        assertSame(
                ExternalProcessProtocolCodec.getSupportedTransportProtocolCapabilities(),
                ExternalProcessProtocolCodec.getSupportedTransportProtocolCapabilities()
        );
        assertSame(
                ExternalProcessProtocolCodec.getSupportedTransportSerializerContractIds(),
                ExternalProcessProtocolCodec.getSupportedTransportSerializerContractIds()
        );
    }

    @Test
    @DisplayName("Should round-trip external process response payload")
    void shouldRoundTripExternalProcessResponsePayload() throws Exception {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put("state", SampleState.READY);
        metadata.put("trace", new int[]{4, 2});

        Map<String, Long> executorCacheStatistics = new HashMap<String, Long>();
        executorCacheStatistics.put("hits", 3L);

        List<String> capabilities = ExternalProcessProtocolCodec.getSupportedTransportProtocolCapabilities();
        List<String> serializerContracts = Collections.singletonList("json-v1");
        ExternalProcessExecutionResponse response = new ExternalProcessExecutionResponse(
                true,
                "SUCCESS",
                SampleState.READY,
                null,
                metadata,
                25L,
                executorCacheStatistics,
                ExternalProcessProtocolCodec.getProtocolVersion(),
                capabilities,
                serializerContracts
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ExternalProcessProtocolCodec.writeResponse(outputStream, response);
        ExternalProcessExecutionResponse decoded = ExternalProcessProtocolCodec.readResponse(
                new ByteArrayInputStream(outputStream.toByteArray())
        );

        assertTrue(decoded.isSuccess());
        assertEquals("SUCCESS", decoded.getStatus());
        assertEquals("READY", decoded.getValue());
        assertEquals(Arrays.<Object>asList(4, 2), decoded.getMetadata().get("trace"));
        assertEquals("READY", decoded.getMetadata().get("state"));
        assertEquals(Long.valueOf(3L), decoded.getExecutorCacheStatistics().get("hits"));
        assertEquals(capabilities, decoded.getSupportedTransportProtocolCapabilities());
        assertEquals(serializerContracts, decoded.getSupportedTransportSerializerContractIds());
    }

    @Test
    @DisplayName("Should reject oversized protocol frames")
    void shouldRejectOversizedProtocolFrames() throws Exception {
        int limit = resolveMaxFrameBytes();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeInt(limit + 1);
        dataOutputStream.flush();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        assertThrows(IOException.class, () -> ExternalProcessProtocolCodec.readResponse(inputStream));
    }

    private enum SampleState {
        READY
    }

    private int resolveMaxFrameBytes() throws Exception {
        Field field = ExternalProcessProtocolCodec.class.getDeclaredField("MAX_FRAME_BYTES");
        field.setAccessible(true);
        return (Integer) field.get(null);
    }
}
