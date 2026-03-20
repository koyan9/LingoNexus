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
import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.api.result.ExecutionStatus;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportProtocolCapability;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Script Executor Feature Tests")
class ExternalProcessScriptExecutorFeatureTest {



    @Test
    @DisplayName("Should map successful worker response and cache statistics")
    void shouldMapSuccessfulWorkerResponseAndCacheStatistics() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new SuccessfulWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient returnedWorker) {
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient discardedWorker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertTrue(result.isSuccess());
            assertEquals("SUCCESS", result.getStatus().name());
            assertEquals("ok", result.getMetadata().get("marker"));
            assertEquals(Long.valueOf(7L), executor.getExecutorCacheStatistics().get("executorCacheHits"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should skip handshake ping when protocol version already exists and no requirements")
    void shouldSkipHandshakePingWhenProtocolVersionAlreadyExistsAndNoRequirements() {
        NegotiatedWorker worker = new NegotiatedWorker();
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return worker;
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient returnedWorker) {
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient discardedWorker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertTrue(result.isSuccess());
            assertEquals(0, worker.pingCount);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify startup failure when worker borrow fails with IO error")
    void shouldClassifyStartupFailureWhenWorkerBorrowFailsWithIoError() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new IOException("simulated startup failure");
                }
        );
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("borrow_worker", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-pool", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_startup_failed", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("IOException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_startup_failed", executor.getLatestBorrowFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_startup_failed"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify borrow timeout failure")
    void shouldClassifyBorrowTimeoutFailure() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() throws IOException, InterruptedException {
                throw new ExternalProcessWorkerBorrowTimeoutException("simulated timeout");
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("borrow_worker", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-pool", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_borrow_timeout", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ExternalProcessWorkerBorrowTimeoutException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_borrow_timeout", executor.getLatestBorrowFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_borrow_timeout"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify interrupted borrow failure and preserve interrupt status")
    void shouldClassifyInterruptedBorrowFailureAndPreserveInterruptStatus() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() throws IOException, InterruptedException {
                throw new InterruptedException("simulated interrupt");
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("borrow_worker", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-pool", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_borrow_interrupted", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("InterruptedException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_borrow_interrupted", executor.getLatestBorrowFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_borrow_interrupted"));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify borrow failure after worker pool shutdown")
    void shouldClassifyBorrowFailureAfterWorkerPoolShutdown() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        );
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            workerPool.shutdown();
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("borrow_worker", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-pool", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_pool_shutdown", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("IllegalStateException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_pool_shutdown", executor.getLatestBorrowFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_pool_shutdown"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify unexpected worker termination during execution")
    void shouldClassifyUnexpectedWorkerTerminationDuringExecution() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> new TerminatingWorker()
        );
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("worker_execution", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_terminated_unexpectedly", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ScriptRuntimeException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_terminated_unexpectedly", executor.getLatestWorkerExecutionFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_terminated_unexpectedly"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify protocol handshake failure before execution")
    void shouldClassifyProtocolHandshakeFailureBeforeExecution() {
        HandshakeFailureWorker worker = new HandshakeFailureWorker();
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return worker;
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("protocol_negotiation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-handshake", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("protocol_handshake_failed", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ExternalProcessCompatibilityException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertTrue(executor.getLatestProtocolNegotiationFailureReason().contains("health check did not complete"));
            assertEquals(1L, executor.getFailureReasonCounts().get("protocol_handshake_failed"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify worker unavailable during execution")
    void shouldClassifyWorkerUnavailableDuringExecution() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new UnavailableWorker();
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("worker_execution", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_unavailable", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ScriptRuntimeException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_unavailable", executor.getLatestWorkerExecutionFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_unavailable"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify generic worker execution failure")
    void shouldClassifyGenericWorkerExecutionFailure() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new GenericFailureWorker();
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertEquals("worker_execution", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_execution_failed", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ScriptRuntimeException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertEquals("worker_execution_failed", executor.getLatestWorkerExecutionFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_execution_failed"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify protocol capability mismatch during negotiation")
    void shouldClassifyProtocolCapabilityMismatchDuringNegotiation() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new CapabilityMismatchWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(buildRequestWithRequirements(
                    EnumSet.of(SandboxTransportProtocolCapability.JSON_FRAMED),
                    Collections.<String>emptySet()
            ), 1000L);

            assertFalse(result.isSuccess());
            assertEquals("protocol_negotiation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-handshake", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("protocol_capability_mismatch", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ExternalProcessCompatibilityException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertTrue(executor.getLatestProtocolNegotiationFailureReason().contains("required capabilities"));
            assertEquals(1L, executor.getFailureReasonCounts().get("protocol_capability_mismatch"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should classify serializer contract mismatch during negotiation")
    void shouldClassifySerializerContractMismatchDuringNegotiation() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new CapabilityMismatchWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }

            @Override
            public void discardWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(buildRequestWithRequirements(
                    Collections.<SandboxTransportProtocolCapability>emptySet(),
                    Collections.singleton("json-v1")
            ), 1000L);

            assertFalse(result.isSuccess());
            assertEquals("protocol_negotiation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-handshake", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("serializer_contract_mismatch", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("ExternalProcessCompatibilityException", result.getMetadata().get(ResultMetadataKeys.ERROR_TYPE));
            assertTrue(executor.getLatestProtocolNegotiationFailureReason().contains("required serializer contracts"));
            assertEquals(1L, executor.getFailureReasonCounts().get("serializer_contract_mismatch"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should ignore worker response failure without reason metadata")
    void shouldIgnoreWorkerResponseFailureWithoutReasonMetadata() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new NoMetadataFailureWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals(ExecutionStatus.FAILURE, result.getStatus());
            assertTrue(result.getMetadata().isEmpty());
            assertTrue(executor.getFailureReasonCounts().isEmpty());
            assertEquals(null, executor.getLatestWorkerExecutionFailureReason());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should track protocol negotiation failure returned in worker metadata without polluting worker execution latest")
    void shouldTrackProtocolNegotiationFailureReturnedInWorkerMetadata() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new ProtocolStageFailureWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals("protocol_negotiation", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-handshake", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("protocol_handshake_failed", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals(1L, executor.getFailureReasonCounts().get("protocol_handshake_failed"));
            assertEquals("simulated handshake failure", executor.getLatestProtocolNegotiationFailureReason());
            assertEquals(1L, executor.getProtocolNegotiationFailureCount());
            assertEquals(null, executor.getLatestWorkerExecutionFailureReason());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should track borrow failure returned in worker metadata without polluting worker execution latest")
    void shouldTrackBorrowFailureReturnedInWorkerMetadata() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new BorrowStageFailureWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals("borrow_worker", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker-pool", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("worker_pool_shutdown", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals(1L, executor.getFailureReasonCounts().get("worker_pool_shutdown"));
            assertEquals("worker_pool_shutdown", executor.getLatestBorrowFailureReason());
            assertEquals(null, executor.getLatestWorkerExecutionFailureReason());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should record worker response failure reason from metadata")
    void shouldRecordWorkerResponseFailureReasonFromMetadata() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new ResponseFailureWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);

            assertFalse(result.isSuccess());
            assertEquals("worker_execution", result.getMetadata().get(ResultMetadataKeys.ERROR_STAGE));
            assertEquals("external-worker", result.getMetadata().get(ResultMetadataKeys.ERROR_COMPONENT));
            assertEquals("security_policy_descriptor_load_failed", result.getMetadata().get(ResultMetadataKeys.ERROR_REASON));
            assertEquals("security_policy_descriptor_load_failed", executor.getLatestWorkerExecutionFailureReason());
            assertEquals(1L, executor.getFailureReasonCounts().get("security_policy_descriptor_load_failed"));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("Should cap failure reason keys to avoid unbounded growth")
    void shouldCapFailureReasonKeysToAvoidUnboundedGrowth() {
        ExternalProcessWorkerPool workerPool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new AssertionError("Worker factory should not be used in this scenario");
                }
        ) {
            @Override
            public ExternalProcessWorkerClient borrowWorker() {
                return new UniqueReasonFailureWorker();
            }

            @Override
            public void returnWorker(ExternalProcessWorkerClient worker) {
            }
        };
        ExternalProcessScriptExecutor executor = new ExternalProcessScriptExecutor(workerPool);
        try {
            for (int i = 0; i < 200; i++) {
                ScriptResult result = executor.execute(ExternalProcessExecutionRequest.healthCheck(), 1000L);
                assertFalse(result.isSuccess());
            }
            Map<String, Long> counts = executor.getFailureReasonCounts();
            assertTrue(counts.size() <= 129);
            assertTrue(counts.get("failure_reason_overflow") != null
                    && counts.get("failure_reason_overflow") > 0L);
        } finally {
            executor.shutdown();
        }
    }


    private static final class SuccessfulWorker extends ExternalProcessWorkerClient {

        private SuccessfulWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public java.util.List<String> getSupportedTransportProtocolCapabilities() {
            return Collections.emptyList();
        }

        @Override
        public java.util.List<String> getSupportedTransportSerializerContractIds() {
            return Collections.emptyList();
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            java.util.Map<String, Object> metadata = new java.util.HashMap<String, Object>();
            metadata.put("marker", "ok");
            java.util.Map<String, Long> cacheStats = new java.util.HashMap<String, Long>();
            cacheStats.put("executorCacheHits", 7L);
            return new ExternalProcessExecutionResponse(true, "SUCCESS", Boolean.TRUE, null, metadata, 5L, cacheStats);
        }
    }

    private static final class NegotiatedWorker extends ExternalProcessWorkerClient {

        private int pingCount;

        private NegotiatedWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public synchronized boolean ping() {
            pingCount++;
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public java.util.List<String> getSupportedTransportProtocolCapabilities() {
            return Collections.emptyList();
        }

        @Override
        public java.util.List<String> getSupportedTransportSerializerContractIds() {
            return Collections.emptyList();
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            return new ExternalProcessExecutionResponse(true, "SUCCESS", Boolean.TRUE, null, Collections.<String, Object>emptyMap(), 0L);
        }
    }


    private static final class TerminatingWorker extends ExternalProcessWorkerClient {

        private TerminatingWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public synchronized boolean ping() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            throw new ScriptRuntimeException("External worker terminated unexpectedly");
        }
    }

    private static final class HandshakeFailureWorker extends ExternalProcessWorkerClient {

        private HandshakeFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public synchronized boolean ping() {
            return false;
        }
    }

    private static final class UnavailableWorker extends ExternalProcessWorkerClient {

        private UnavailableWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return false;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }
    }

    private static final class GenericFailureWorker extends ExternalProcessWorkerClient {

        private GenericFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            throw new ScriptRuntimeException("simulated worker crash");
        }
    }

    private static final class CapabilityMismatchWorker extends ExternalProcessWorkerClient {

        private CapabilityMismatchWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public java.util.List<String> getSupportedTransportProtocolCapabilities() {
            return Collections.emptyList();
        }

        @Override
        public java.util.List<String> getSupportedTransportSerializerContractIds() {
            return Collections.emptyList();
        }
    }

    private static final class ResponseFailureWorker extends ExternalProcessWorkerClient {

        private ResponseFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            java.util.Map<String, Object> metadata = new java.util.HashMap<String, Object>();
            metadata.put(ResultMetadataKeys.ERROR_STAGE, "worker_execution");
            metadata.put(ResultMetadataKeys.ERROR_COMPONENT, "external-worker");
            metadata.put(ResultMetadataKeys.ERROR_REASON, "security_policy_descriptor_load_failed");
            metadata.put(ResultMetadataKeys.ERROR_MESSAGE, "simulated compatibility failure");
            return new ExternalProcessExecutionResponse(false, "FAILURE", null, "simulated compatibility failure", metadata, 5L);
        }
    }

    private static final class NoMetadataFailureWorker extends ExternalProcessWorkerClient {

        private NoMetadataFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            return new ExternalProcessExecutionResponse(false, "FAILURE", null, "simulated failure", Collections.<String, Object>emptyMap(), 5L);
        }
    }

    private static final class ProtocolStageFailureWorker extends ExternalProcessWorkerClient {

        private ProtocolStageFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            java.util.Map<String, Object> metadata = new java.util.HashMap<String, Object>();
            metadata.put(ResultMetadataKeys.ERROR_STAGE, "protocol_negotiation");
            metadata.put(ResultMetadataKeys.ERROR_COMPONENT, "external-worker-handshake");
            metadata.put(ResultMetadataKeys.ERROR_REASON, "protocol_handshake_failed");
            metadata.put(ResultMetadataKeys.ERROR_MESSAGE, "simulated handshake failure");
            return new ExternalProcessExecutionResponse(false, "FAILURE", null, "simulated handshake failure", metadata, 5L);
        }
    }

    private static final class BorrowStageFailureWorker extends ExternalProcessWorkerClient {

        private BorrowStageFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            java.util.Map<String, Object> metadata = new java.util.HashMap<String, Object>();
            metadata.put(ResultMetadataKeys.ERROR_STAGE, "borrow_worker");
            metadata.put(ResultMetadataKeys.ERROR_COMPONENT, "external-worker-pool");
            metadata.put(ResultMetadataKeys.ERROR_REASON, "worker_pool_shutdown");
            metadata.put(ResultMetadataKeys.ERROR_MESSAGE, "simulated pool shutdown");
            return new ExternalProcessExecutionResponse(false, "FAILURE", null, "simulated pool shutdown", metadata, 5L);
        }
    }

    private static final class UniqueReasonFailureWorker extends ExternalProcessWorkerClient {

        private static final java.util.concurrent.atomic.AtomicInteger sequence = new java.util.concurrent.atomic.AtomicInteger(0);

        private UniqueReasonFailureWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return true;
        }

        @Override
        public String getProtocolVersion() {
            return "1";
        }

        @Override
        public synchronized ExternalProcessExecutionResponse execute(ExternalProcessExecutionRequest request, long timeoutMs) {
            java.util.Map<String, Object> metadata = new java.util.HashMap<String, Object>();
            metadata.put(ResultMetadataKeys.ERROR_STAGE, "worker_execution");
            metadata.put(ResultMetadataKeys.ERROR_COMPONENT, "external-worker");
            metadata.put(ResultMetadataKeys.ERROR_REASON, "reason-" + sequence.incrementAndGet());
            return new ExternalProcessExecutionResponse(false, "FAILURE", null, "simulated failure", metadata, 5L);
        }
    }

    private static ExternalProcessExecutionRequest buildRequestWithRequiredCapabilities(
            Set<SandboxTransportProtocolCapability> requiredCapabilities) {
        return buildRequestWithRequirements(requiredCapabilities, Collections.<String>emptySet());
    }

    private static ExternalProcessExecutionRequest buildRequestWithRequirements(
            Set<SandboxTransportProtocolCapability> requiredCapabilities,
            Set<String> requiredSerializerContracts) {
        return new ExternalProcessExecutionRequest(
                ExternalProcessExecutionRequest.RequestType.HEALTHCHECK,
                null,
                null,
                null,
                false,
                0L,
                0L,
                0L,
                1,
                0L,
                false,
                0,
                false,
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Collections.<String>emptySet(),
                Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostAccessMode>emptySet(),
                Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionMode>emptySet(),
                Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxHostRestrictionFlag>emptySet(),
                Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxResultTransportMode>emptySet(),
                Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportSerializerMode>emptySet(),
                Collections.<io.github.koyan9.lingonexus.api.sandbox.spi.SandboxTransportPayloadProfile>emptySet(),
                requiredCapabilities != null ? requiredCapabilities : Collections.<SandboxTransportProtocolCapability>emptySet(),
                requiredSerializerContracts != null ? requiredSerializerContracts : Collections.<String>emptySet(),
                false,
                false,
                false,
                false,
                io.github.koyan9.lingonexus.api.result.ResultMetadataProfile.FULL,
                Collections.<io.github.koyan9.lingonexus.api.result.ResultMetadataCategory>emptySet(),
                Collections.<ExternalProcessExtensionDescriptor>emptyList(),
                Collections.<ExternalProcessExtensionDescriptor>emptyList(),
                Collections.<String, Object>emptyMap(),
                Collections.<String, Object>emptyMap()
        );
    }
}
