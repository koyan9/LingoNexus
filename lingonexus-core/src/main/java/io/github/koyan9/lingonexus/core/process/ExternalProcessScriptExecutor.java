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
import io.github.koyan9.lingonexus.api.exception.ExternalProcessCompatibilityException;
import io.github.koyan9.lingonexus.api.exception.ScriptTimeoutException;
import io.github.koyan9.lingonexus.api.lifecycle.LifecycleAware;
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ScriptResult;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Executes scripts in a persistent external JVM worker pool.
 */
public class ExternalProcessScriptExecutor implements LifecycleAware {

    private static final String STAGE_REQUEST_VALIDATION = "request_validation";
    private static final String STAGE_PROTOCOL_NEGOTIATION = "protocol_negotiation";
    private static final String STAGE_BORROW_WORKER = "borrow_worker";
    private static final String STAGE_WORKER_EXECUTION = "worker_execution";

    private static final String COMPONENT_EXTERNAL_PROCESS_COMPATIBILITY = "external-process-compatibility";
    private static final String COMPONENT_EXTERNAL_WORKER_HANDSHAKE = "external-worker-handshake";
    private static final String COMPONENT_EXTERNAL_WORKER_POOL = "external-worker-pool";
    private static final String COMPONENT_EXTERNAL_WORKER = "external-worker";

    private final ExternalProcessWorkerPool workerPool;
    private final AtomicLong protocolNegotiationFailureCount = new AtomicLong(0L);
    private final ConcurrentHashMap<String, AtomicLong> failureReasonCounts = new ConcurrentHashMap<String, AtomicLong>();
    private volatile Map<String, Long> lastExecutorCacheStatistics = Collections.emptyMap();
    private volatile String latestProtocolNegotiationFailureReason;
    private volatile String latestBorrowFailureReason;
    private volatile String latestWorkerExecutionFailureReason;

    public ExternalProcessScriptExecutor(int poolSize, int startupRetries, int prewarmCount, long idleTtlMs) {
        this.workerPool = new ExternalProcessWorkerPool(
                resolveJavaCommand(),
                resolveClasspath(),
                poolSize,
                startupRetries,
                prewarmCount,
                idleTtlMs
        );
    }

    ExternalProcessScriptExecutor(ExternalProcessWorkerPool workerPool) {
        this.workerPool = workerPool;
    }

    public ScriptResult execute(ExternalProcessExecutionRequest request, long timeoutMs) {
        ExternalProcessWorkerClient worker = null;
        try {
            validateJsonSafeRequest(request);
            worker = workerPool.borrowWorker();
            validateNegotiatedTransport(worker, request);
            ExternalProcessExecutionResponse response = worker.execute(request, timeoutMs);
            updateExecutorCacheStatistics(response);
            workerPool.returnWorker(worker);
            return toScriptResult(response);
        } catch (ClassifiedCompatibilityException e) {
            if (!STAGE_PROTOCOL_NEGOTIATION.equals(e.getStage())) {
                recordFailureReason(e.getReason());
            }
            if (worker != null && worker.isAlive()) {
                workerPool.returnWorker(worker);
            } else {
                workerPool.discardWorker(worker);
            }
            return failureResult(e, e.getStage(), e.getComponent(), e.getReason(), 0L, ExecutionStatus.FAILURE);
        } catch (ExternalProcessCompatibilityException e) {
            String failureStage = worker == null ? STAGE_REQUEST_VALIDATION : STAGE_PROTOCOL_NEGOTIATION;
            String failureComponent = worker == null
                    ? COMPONENT_EXTERNAL_PROCESS_COMPATIBILITY
                    : COMPONENT_EXTERNAL_WORKER_HANDSHAKE;
            String failureReason = worker == null
                    ? "external_process_compatibility_failure"
                    : "protocol_negotiation_failure";
            if (e.getStage() != null) {
                failureStage = e.getStage();
            }
            if (e.getComponent() != null) {
                failureComponent = e.getComponent();
            }
            if (e.getReason() != null) {
                failureReason = e.getReason();
            }
            if (STAGE_PROTOCOL_NEGOTIATION.equals(failureStage)) {
                latestProtocolNegotiationFailureReason = e.getMessage();
                protocolNegotiationFailureCount.incrementAndGet();
            }
            if (STAGE_BORROW_WORKER.equals(failureStage)) {
                latestBorrowFailureReason = failureReason;
            }
            if (STAGE_WORKER_EXECUTION.equals(failureStage)) {
                latestWorkerExecutionFailureReason = failureReason;
            }
            recordFailureReason(failureReason);
            if (worker != null && worker.isAlive()) {
                workerPool.returnWorker(worker);
            } else {
                workerPool.discardWorker(worker);
            }
            return failureResult(
                    e,
                    failureStage,
                    failureComponent,
                    failureReason,
                    0L,
                    ExecutionStatus.FAILURE
            );
        } catch (ScriptTimeoutException e) {
            latestWorkerExecutionFailureReason = "worker_execution_timeout";
            recordFailureReason("worker_execution_timeout");
            workerPool.discardWorker(worker);
            return failureResult(e, STAGE_WORKER_EXECUTION, COMPONENT_EXTERNAL_WORKER,
                    "worker_execution_timeout", e.getActualExecutionTimeMs(), ExecutionStatus.TIMEOUT);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            workerPool.discardWorker(worker);
            String failureReason = worker == null ? classifyBorrowFailureReason(e) : classifyWorkerExecutionFailureReason(e);
            if (worker == null) {
                latestBorrowFailureReason = failureReason;
            } else {
                latestWorkerExecutionFailureReason = failureReason;
            }
            recordFailureReason(failureReason);
            return failureResult(
                    e,
                    worker == null ? STAGE_BORROW_WORKER : STAGE_WORKER_EXECUTION,
                    worker == null ? COMPONENT_EXTERNAL_WORKER_POOL : COMPONENT_EXTERNAL_WORKER,
                    failureReason,
                    0L,
                    ExecutionStatus.FAILURE
            );
        }
    }

    @Override
    public void shutdown() {
        workerPool.shutdown();
    }

    @Override
    public boolean isShutdown() {
        return workerPool.isShutdown();
    }

    public ExternalProcessWorkerPoolStatistics getStatistics() {
        return workerPool.getStatistics();
    }

    public Map<String, Long> getExecutorCacheStatistics() {
        return new HashMap<String, Long>(lastExecutorCacheStatistics);
    }


    public long getProtocolNegotiationFailureCount() {
        return protocolNegotiationFailureCount.get();
    }

    public String getLatestProtocolNegotiationFailureReason() {
        return latestProtocolNegotiationFailureReason;
    }

    public String getLatestBorrowFailureReason() {
        return latestBorrowFailureReason;
    }

    public String getLatestWorkerExecutionFailureReason() {
        return latestWorkerExecutionFailureReason;
    }

    public Map<String, Long> getFailureReasonCounts() {
        Map<String, Long> snapshot = new LinkedHashMap<String, Long>();
        for (Map.Entry<String, AtomicLong> entry : failureReasonCounts.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return snapshot;
    }


    public ExternalProcessWorkerPool.ProtocolHandshakeSnapshot getProtocolHandshakeSnapshot() {
        return workerPool.getProtocolHandshakeSnapshot();
    }

    private void validateNegotiatedTransport(ExternalProcessWorkerClient worker, ExternalProcessExecutionRequest request) {
        if (worker == null) {
            return;
        }
        if (worker.getProtocolVersion() == null && !worker.ping()) {
            throw protocolNegotiationFailure(
                    "protocol_handshake_failed",
                    "External worker protocol negotiation failed: health check did not complete"
            );
        }

        java.util.List<String> requiredProtocolCapabilities = toRequiredProtocolCapabilityNames(
                request.getRequiredSandboxTransportProtocolCapabilities()
        );
        java.util.List<String> supportedProtocolCapabilities = worker.getSupportedTransportProtocolCapabilities();
        if (!requiredProtocolCapabilities.isEmpty() && !supportedProtocolCapabilities.containsAll(requiredProtocolCapabilities)) {
            throw protocolNegotiationFailure(
                    "protocol_capability_mismatch",
                    "External worker protocol negotiation failed: required capabilities " + requiredProtocolCapabilities
                            + " are not supported by worker " + supportedProtocolCapabilities
            );
        }

        Set<String> requiredSerializerContracts = request.getRequiredSandboxTransportSerializerContractIds();
        java.util.List<String> supportedSerializerContracts = worker.getSupportedTransportSerializerContractIds();
        if (requiredSerializerContracts != null && !requiredSerializerContracts.isEmpty()
                && !supportedSerializerContracts.containsAll(requiredSerializerContracts)) {
            throw protocolNegotiationFailure(
                    "serializer_contract_mismatch",
                    "External worker protocol negotiation failed: required serializer contracts " + requiredSerializerContracts
                            + " are not supported by worker " + supportedSerializerContracts
            );
        }
    }

    private java.util.List<String> toRequiredProtocolCapabilityNames(
            Set<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability> requiredCapabilities) {
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.List<String> result = new java.util.ArrayList<String>(requiredCapabilities.size());
        for (io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability capability : requiredCapabilities) {
            if (capability != null) {
                result.add(capability.name());
            }
        }
        return result;
    }

    private void updateExecutorCacheStatistics(ExternalProcessExecutionResponse response) {
        if (response != null && response.getExecutorCacheStatistics() != null) {
            lastExecutorCacheStatistics = new HashMap<String, Long>(response.getExecutorCacheStatistics());
        }
    }

    private ScriptResult toScriptResult(ExternalProcessExecutionResponse response) {
        Map<String, Object> metadata = response.getMetadata() != null
                ? new HashMap<String, Object>(response.getMetadata())
                : new HashMap<String, Object>();

        ExecutionStatus status = response.getStatus() != null
                ? ExecutionStatus.valueOf(response.getStatus())
                : (response.isSuccess() ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILURE);

        return ScriptResult.of(
                status,
                response.getValue(),
                response.getErrorMessage(),
                null,
                response.getExecutionTime(),
                metadata
        );
    }

    private void validateJsonSafeRequest(ExternalProcessExecutionRequest request) throws IOException {
        try {
            if (request.getVariables() != null) {
                JsonSafeValueNormalizer.normalizeMap(request.getVariables(), "$.variables", false);
            }
            if (request.getMetadata() != null) {
                JsonSafeValueNormalizer.normalizeMap(request.getMetadata(), "$.metadata", false);
            }
        } catch (IllegalArgumentException e) {
            throw new ClassifiedCompatibilityException(
                    "External process mode requires JSON-safe request payload: " + e.getMessage(),
                    e,
                    STAGE_REQUEST_VALIDATION,
                    COMPONENT_EXTERNAL_PROCESS_COMPATIBILITY,
                    "request_payload_not_json_safe"
            );
        }
    }

    private ScriptResult failureResult(Exception e, String stage, String component, String reason,
                                       long executionTime, ExecutionStatus status) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        metadata.put(ResultMetadataKeys.ERROR_TYPE, resolveErrorType(e));
        metadata.put(ResultMetadataKeys.ERROR_MESSAGE, e.getMessage());
        metadata.put(ResultMetadataKeys.ERROR_STAGE, stage);
        metadata.put(ResultMetadataKeys.ERROR_COMPONENT, component);
        metadata.put(ResultMetadataKeys.ERROR_REASON, reason);
        return ScriptResult.of(status, null, e.getMessage(), null, executionTime, metadata);
    }

    private ClassifiedCompatibilityException protocolNegotiationFailure(String reason, String message) {
        protocolNegotiationFailureCount.incrementAndGet();
        latestProtocolNegotiationFailureReason = message;
        recordFailureReason(reason);
        return new ClassifiedCompatibilityException(
                message,
                STAGE_PROTOCOL_NEGOTIATION,
                COMPONENT_EXTERNAL_WORKER_HANDSHAKE,
                reason
        );
    }

    private String classifyBorrowFailureReason(Exception e) {
        if (e instanceof InterruptedException) {
            return "worker_borrow_interrupted";
        }
        if (e instanceof IOException) {
            return "worker_startup_failed";
        }
        if (e instanceof IllegalStateException && e.getMessage() != null && e.getMessage().toLowerCase().contains("shut down")) {
            return "worker_pool_shutdown";
        }
        return "worker_borrow_failed";
    }

    private String classifyWorkerExecutionFailureReason(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        if (message.contains("terminated unexpectedly")) {
            return "worker_terminated_unexpectedly";
        }
        if (message.contains("not available")) {
            return "worker_unavailable";
        }
        return "worker_execution_failed";
    }

    private String resolveErrorType(Exception e) {
        if (e instanceof ExternalProcessCompatibilityException) {
            return ExternalProcessCompatibilityException.class.getSimpleName();
        }
        return e.getClass().getSimpleName();
    }

    private void recordFailureReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return;
        }
        failureReasonCounts.computeIfAbsent(reason, key -> new AtomicLong(0L)).incrementAndGet();
    }

    private static final class ClassifiedCompatibilityException extends ExternalProcessCompatibilityException {

        private final String stage;
        private final String component;
        private final String reason;

        private ClassifiedCompatibilityException(String message, String stage, String component, String reason) {
            super(message);
            this.stage = stage;
            this.component = component;
            this.reason = reason;
        }

        private ClassifiedCompatibilityException(String message, Throwable cause, String stage, String component, String reason) {
            super(message, cause);
            this.stage = stage;
            this.component = component;
            this.reason = reason;
        }

        @Override
        public String getStage() {
            return stage;
        }

        @Override
        public String getComponent() {
            return component;
        }

        @Override
        public String getReason() {
            return reason;
        }
    }

    private String resolveJavaCommand() {
        String javaHome = System.getProperty("java.home");
        if (javaHome != null && !javaHome.isEmpty()) {
            return Path.of(javaHome, "bin", isWindows() ? "java.exe" : "java").toString();
        }
        return isWindows() ? "java.exe" : "java";
    }

    private String resolveClasspath() {
        String classpath = System.getProperty("java.class.path");
        if (classpath == null || classpath.trim().isEmpty()) {
            throw new IllegalStateException("Current process classpath is empty, cannot launch external worker");
        }
        return classpath;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }
}
