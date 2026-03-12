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
import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.exception.ExternalProcessCompatibilityException;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.process.ExternalProcessDescriptorConsumer;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.security.SecurityPolicy;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Worker entry point for one-shot external process execution.
 */
public final class ExternalProcessWorkerMain {

    private static final Logger log = LoggerFactory.getLogger(ExternalProcessWorkerMain.class);

    private ExternalProcessWorkerMain() {
    }

    public static void main(String[] args) {
        if (args.length == 2 && "--socket".equals(args[0])) {
            runSocketServer(Integer.parseInt(args[1]));
            return;
        }

        if (args.length < 2) {
            System.exit(2);
        }

        String requestFile = args[0];
        String responseFile = args[1];

        ExternalProcessExecutionResponse response;
        try {
            ExternalProcessExecutionRequest request = readRequest(requestFile);
            response = executeRequest(request);
        } catch (Exception e) {
            log.error("Failed to execute external process request", e);
            Map<String, Object> errorMetadata = buildGenericFailureMetadata(
                    e,
                    "worker_execution",
                    "external-worker",
                    "worker_execution_failed"
            );
            response = new ExternalProcessExecutionResponse(
                    false,
                    "FAILURE",
                    null,
                    e.getMessage(),
                    errorMetadata,
                    0L,
                    ExternalProcessExecutorRegistry.getStatistics()
            );
        }

        try {
            writeResponse(responseFile, response);
        } catch (Exception e) {
            log.error("Failed to write external process response", e);
            System.exit(1);
        }
    }

    private static void runSocketServer(int port) {
        try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), port)) {
            BufferedOutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
            BufferedInputStream inputStream = new BufferedInputStream(socket.getInputStream());

            while (true) {
                try {
                    ExternalProcessExecutionRequest request = ExternalProcessProtocolCodec.readRequest(inputStream);
                    ExternalProcessExecutionResponse response = executeRequest(request);
                    ExternalProcessProtocolCodec.writeResponse(outputStream, response);
                } catch (EOFException e) {
                    break;
                } catch (SocketException e) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("External process socket server terminated unexpectedly", e);
            System.exit(1);
        } finally {
            ExternalProcessExecutorRegistry.shutdownAll();
        }
    }

    private static ExternalProcessExecutionRequest readRequest(String requestFile) throws Exception {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(requestFile))) {
            return (ExternalProcessExecutionRequest) inputStream.readObject();
        }
    }

    private static void writeResponse(String responseFile, ExternalProcessExecutionResponse response) throws Exception {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(responseFile))) {
            outputStream.writeObject(response);
        }
    }

    private static ExternalProcessExecutionResponse executeRequest(ExternalProcessExecutionRequest request) {
        if (request.getRequestType() == ExternalProcessExecutionRequest.RequestType.HEALTHCHECK) {
            return new ExternalProcessExecutionResponse(true, "SUCCESS", Boolean.TRUE, null, Collections.<String, Object>emptyMap(), 0L, ExternalProcessExecutorRegistry.getStatistics(), ExternalProcessProtocolCodec.getProtocolVersion(), ExternalProcessProtocolCodec.getSupportedTransportProtocolCapabilities(), ExternalProcessProtocolCodec.getSupportedTransportSerializerContractIds());
        }

        try {
            LingoNexusExecutor executor = ExternalProcessExecutorRegistry.getOrCreate(request);
            ScriptContext context = ScriptContext.of(
                    request.getVariables() != null ? request.getVariables() : Collections.<String, Object>emptyMap(),
                    request.getMetadata()
            );
            ScriptResult result = executor.execute(request.getScript(), request.getLanguage(), context);

            return buildExecutionResponse(result);
        } catch (ExternalProcessCompatibilityException e) {
            Map<String, Object> errorMetadata = new HashMap<String, Object>();
            String stage = e.getStage() != null ? e.getStage() : "worker_execution";
            String component = e.getComponent() != null ? e.getComponent() : "external-worker";
            String reason = e.getReason() != null ? e.getReason() : "external_process_compatibility_failure";
            errorMetadata.put(ResultMetadataKeys.ERROR_TYPE, "ExternalProcessCompatibilityException");
            errorMetadata.put(ResultMetadataKeys.ERROR_MESSAGE, e.getMessage());
            errorMetadata.put(ResultMetadataKeys.ERROR_STAGE, stage);
            errorMetadata.put(ResultMetadataKeys.ERROR_COMPONENT, component);
            errorMetadata.put(ResultMetadataKeys.ERROR_REASON, reason);
            return new ExternalProcessExecutionResponse(
                    false,
                    "FAILURE",
                    null,
                    e.getMessage(),
                    errorMetadata,
                    0L,
                    ExternalProcessExecutorRegistry.getStatistics()
            );
        } catch (Exception e) {
            Map<String, Object> errorMetadata = buildGenericFailureMetadata(
                    e,
                    "worker_execution",
                    "external-worker",
                    "worker_execution_failed"
            );
            return new ExternalProcessExecutionResponse(
                    false,
                    "FAILURE",
                    null,
                    e.getMessage(),
                    errorMetadata,
                    0L,
                    ExternalProcessExecutorRegistry.getStatistics()
            );
        }
    }

    static ExternalProcessExecutionResponse buildExecutionResponse(ScriptResult result) {
        if (result == null) {
            return new ExternalProcessExecutionResponse(
                    false,
                    "FAILURE",
                    null,
                    "External process response is null",
                    Collections.<String, Object>emptyMap(),
                    0L,
                    ExternalProcessExecutorRegistry.getStatistics()
            );
        }

        try {
            Object normalizedValue = JsonSafeValueNormalizer.normalizeForResponse(result.getValue());
            Map<String, Object> normalizedMetadata = result.getMetadata() != null
                    ? JsonSafeValueNormalizer.normalizeMap(result.getMetadata(), "$.metadata", true)
                    : Collections.<String, Object>emptyMap();

            return new ExternalProcessExecutionResponse(
                    result.isSuccess(),
                    result.getStatus().name(),
                    normalizedValue,
                    result.getErrorMessage(),
                    normalizedMetadata,
                    result.getExecutionTime(),
                    ExternalProcessExecutorRegistry.getStatistics()
            );
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorMetadata = new HashMap<String, Object>();
            errorMetadata.put(ResultMetadataKeys.ERROR_TYPE, "ExternalProcessCompatibilityException");
            errorMetadata.put(ResultMetadataKeys.ERROR_STAGE, "worker_execution");
            errorMetadata.put(ResultMetadataKeys.ERROR_COMPONENT, "external-worker");
            errorMetadata.put(ResultMetadataKeys.ERROR_REASON, "response_payload_not_json_safe");
            String message = "EXTERNAL_PROCESS requires response payload compatible with JSON transport: " + e.getMessage();
            errorMetadata.put(ResultMetadataKeys.ERROR_MESSAGE, message);
            populateJsonSafeDiagnostics(errorMetadata, e);
            return new ExternalProcessExecutionResponse(
                    false,
                    "FAILURE",
                    null,
                    message,
                    errorMetadata,
                    result.getExecutionTime(),
                    ExternalProcessExecutorRegistry.getStatistics()
            );
        }
    }

    private static void populateJsonSafeDiagnostics(Map<String, Object> metadata, Throwable error) {
        JsonSafeValueException jsonSafeFailure = JsonSafeValueException.find(error);
        if (jsonSafeFailure == null) {
            return;
        }
        if (jsonSafeFailure.getPath() != null) {
            metadata.put(ResultMetadataKeys.ERROR_PATH, jsonSafeFailure.getPath());
        }
        if (jsonSafeFailure.getValueType() != null) {
            metadata.put(ResultMetadataKeys.ERROR_VALUE_TYPE, jsonSafeFailure.getValueType());
        }
        if (jsonSafeFailure.getDetailReason() != null) {
            metadata.put(ResultMetadataKeys.ERROR_DETAIL_REASON, jsonSafeFailure.getDetailReason());
        }
    }

    private static Map<String, Object> buildGenericFailureMetadata(Exception e, String stage,
                                                                   String component, String reason) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put(ResultMetadataKeys.ERROR_TYPE, e != null ? e.getClass().getSimpleName() : "Exception");
        metadata.put(ResultMetadataKeys.ERROR_MESSAGE, e != null ? e.getMessage() : null);
        metadata.put(ResultMetadataKeys.ERROR_STAGE, stage);
        metadata.put(ResultMetadataKeys.ERROR_COMPONENT, component);
        metadata.put(ResultMetadataKeys.ERROR_REASON, reason);
        return metadata;
    }

}
