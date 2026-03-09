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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("External Process Script Executor Feature Tests")
class ExternalProcessScriptExecutorFeatureTest {

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
}
