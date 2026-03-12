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

import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Worker Main Feature Tests")
class ExternalProcessWorkerMainFeatureTest {

    @Test
    @DisplayName("Should classify response metadata with non-string keys as transport incompatible")
    void shouldClassifyResponseMetadataWithNonStringKeysAsTransportIncompatible() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        Map<Object, Object> badNested = new LinkedHashMap<Object, Object>();
        badNested.put(Integer.valueOf(1), "bad-key");
        metadata.put("bad", badNested);

        ScriptResult result = ScriptResult.of(
                ExecutionStatus.SUCCESS,
                "ok",
                null,
                null,
                12L,
                metadata
        );

        ExternalProcessExecutionResponse response = ExternalProcessWorkerMain.buildExecutionResponse(result);

        assertFalse(response.isSuccess());
        assertEquals("FAILURE", response.getStatus());
        assertNotNull(response.getMetadata());
        assertEquals("ExternalProcessCompatibilityException", response.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
        assertEquals("worker_execution", response.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
        assertEquals("external-worker", response.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
        assertEquals("response_payload_not_json_safe", response.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
        assertEquals("$.metadata.bad", response.getMetadata().get(ResultMetadataKeys.ERROR_PATH));
        assertEquals(Integer.class.getName(), response.getMetadata().get(ResultMetadataKeys.ERROR_VALUE_TYPE));
        assertEquals("map_key_not_string", response.getMetadata().get(ResultMetadataKeys.ERROR_DETAIL_REASON));
        assertTrue(String.valueOf(response.getErrorMessage()).contains("response payload compatible with JSON transport"));
    }

    @Test
    @DisplayName("Should classify nested response metadata with non-string keys as transport incompatible")
    void shouldClassifyNestedResponseMetadataWithNonStringKeysAsTransportIncompatible() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        java.util.List<Object> trace = new java.util.ArrayList<Object>();
        Map<Object, Object> bad = new LinkedHashMap<Object, Object>();
        bad.put(Integer.valueOf(1), "bad-key");
        trace.add(bad);
        metadata.put("trace", trace);

        ScriptResult result = ScriptResult.of(
                ExecutionStatus.SUCCESS,
                "ok",
                null,
                null,
                8L,
                metadata
        );

        ExternalProcessExecutionResponse response = ExternalProcessWorkerMain.buildExecutionResponse(result);

        assertFalse(response.isSuccess());
        assertEquals("FAILURE", response.getStatus());
        assertNotNull(response.getMetadata());
        assertEquals("ExternalProcessCompatibilityException", response.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
        assertEquals("worker_execution", response.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
        assertEquals("external-worker", response.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
        assertEquals("response_payload_not_json_safe", response.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
        assertTrue(String.valueOf(response.getErrorMessage()).contains("$.metadata.trace[0]"));
        assertEquals("$.metadata.trace[0]", response.getMetadata().get(ResultMetadataKeys.ERROR_PATH));
        assertEquals(Integer.class.getName(), response.getMetadata().get(ResultMetadataKeys.ERROR_VALUE_TYPE));
        assertEquals("map_key_not_string", response.getMetadata().get(ResultMetadataKeys.ERROR_DETAIL_REASON));
    }

    @Test
    @DisplayName("Should stringify unsupported response metadata values")
    void shouldStringifyUnsupportedResponseMetadataValues() {
        Map<String, Object> metadata = new HashMap<String, Object>();
        Object marker = new Object();
        metadata.put("note", marker);

        ScriptResult result = ScriptResult.of(
                ExecutionStatus.SUCCESS,
                "ok",
                null,
                null,
                6L,
                metadata
        );

        ExternalProcessExecutionResponse response = ExternalProcessWorkerMain.buildExecutionResponse(result);

        assertTrue(response.isSuccess());
        assertEquals("SUCCESS", response.getStatus());
        assertNotNull(response.getMetadata());
        Object noteValue = response.getMetadata().get("note");
        assertTrue(noteValue instanceof String);
        assertTrue(String.valueOf(noteValue).contains(marker.getClass().getName()));
    }
}
