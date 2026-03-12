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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("External Process Worker Pool Feature Tests")
class ExternalProcessWorkerPoolFeatureTest {

    @Test
    @DisplayName("Should count startup failures across retries when worker creation fails")
    void shouldCountStartupFailuresAcrossRetriesWhenWorkerCreationFails() {
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                2,
                0,
                0L,
                (javaCommand, classpath) -> {
                    throw new IOException("simulated startup failure");
                }
        );

        try {
            assertThrows(IOException.class, pool::borrowWorker);
            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(3L, statistics.getStartupFailureCount());
            assertEquals(0, statistics.getCreatedWorkers());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should succeed after retry when startup eventually succeeds")
    void shouldSucceedAfterRetryWhenStartupEventuallySucceeds() throws Exception {
        MutableWorker worker = new MutableWorker();
        AtomicInteger attempts = new AtomicInteger(0);

        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                1,
                0,
                0L,
                (javaCommand, classpath) -> {
                    if (attempts.getAndIncrement() == 0) {
                        throw new IOException("simulated startup failure");
                    }
                    return worker;
                }
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            assertSame(worker, borrowed);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(1L, statistics.getStartupFailureCount());
            assertEquals(1L, statistics.getBorrowCount());
            assertEquals(1, statistics.getCreatedWorkers());

            pool.returnWorker(borrowed);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should count multiple startup failures before eventual success")
    void shouldCountMultipleStartupFailuresBeforeEventualSuccess() throws Exception {
        MutableWorker worker = new MutableWorker();
        AtomicInteger attempts = new AtomicInteger(0);

        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                2,
                0,
                0L,
                (javaCommand, classpath) -> {
                    if (attempts.getAndIncrement() < 2) {
                        throw new IOException("simulated startup failure");
                    }
                    return worker;
                }
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            assertSame(worker, borrowed);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(2L, statistics.getStartupFailureCount());
            assertEquals(1L, statistics.getBorrowCount());
            assertEquals(1, statistics.getCreatedWorkers());

            pool.returnWorker(borrowed);
            assertEquals(1L, pool.getStatistics().getReturnCount());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should track borrow and return counts for successful roundtrip")
    void shouldTrackBorrowAndReturnCountsForSuccessfulRoundtrip() throws Exception {
        MutableWorker worker = new MutableWorker();
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> worker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            pool.returnWorker(borrowed);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(1L, statistics.getBorrowCount());
            assertEquals(1L, statistics.getReturnCount());
            assertEquals(0L, statistics.getDiscardCount());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should reuse worker handshake snapshot without rebuilding")
    void shouldReuseWorkerHandshakeSnapshotWithoutRebuilding() throws Exception {
        MutableWorker worker = new MutableWorker();
        worker.protocolHandshakeSnapshot = ExternalProcessWorkerPool.ProtocolHandshakeSnapshot.of(
                "1",
                Collections.singletonList("JSON_FRAMED"),
                Collections.singletonList("json-v1")
        );

        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> worker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            pool.returnWorker(borrowed);

            ExternalProcessWorkerPool.ProtocolHandshakeSnapshot snapshot = pool.getProtocolHandshakeSnapshot();
            assertSame(worker.protocolHandshakeSnapshot, snapshot);
            assertEquals("1", snapshot.getProtocolVersion());
        } finally {
            pool.shutdown();
        }
    }


    @Test
    @DisplayName("Should return healthy worker without extra ping")
    void shouldReturnHealthyWorkerWithoutExtraPing() throws Exception {
        MutableWorker worker = new MutableWorker();
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> worker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            int pingCountAfterBorrow = worker.pingCount.get();

            pool.returnWorker(borrowed);

            assertEquals(pingCountAfterBorrow, worker.pingCount.get());
            assertEquals(1, pool.getStatistics().getIdleWorkers());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should discard unhealthy idle worker and recover with replacement")
    void shouldDiscardUnhealthyIdleWorkerAndRecoverWithReplacement() throws Exception {
        MutableWorker firstWorker = new MutableWorker();
        MutableWorker secondWorker = new MutableWorker();
        AtomicInteger creationCount = new AtomicInteger(0);

        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> creationCount.getAndIncrement() == 0 ? firstWorker : secondWorker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            assertSame(firstWorker, borrowed);
            pool.returnWorker(borrowed);

            firstWorker.pingResult = false;

            ExternalProcessWorkerClient replacement = pool.borrowWorker();
            assertSame(secondWorker, replacement);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(1L, statistics.getHealthCheckFailureCount());
            assertEquals(1L, statistics.getDiscardCount());
            assertEquals(1, statistics.getCreatedWorkers());

            pool.returnWorker(replacement);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should evict idle worker after TTL and borrow replacement")
    void shouldEvictIdleWorkerAfterTtlAndBorrowReplacement() throws Exception {
        MutableWorker firstWorker = new MutableWorker();
        MutableWorker secondWorker = new MutableWorker();
        AtomicInteger creationCount = new AtomicInteger(0);

        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                1L,
                (javaCommand, classpath) -> creationCount.getAndIncrement() == 0 ? firstWorker : secondWorker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            assertSame(firstWorker, borrowed);
            pool.returnWorker(borrowed);

            Thread.sleep(5L);

            ExternalProcessWorkerClient replacement = pool.borrowWorker();
            assertSame(secondWorker, replacement);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(1L, statistics.getEvictionCount());
            assertEquals(1L, statistics.getDiscardCount());
            assertEquals(1, statistics.getCreatedWorkers());

            pool.returnWorker(replacement);
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should discard dead worker on return and record health-check failure")
    void shouldDiscardDeadWorkerOnReturnAndRecordHealthCheckFailure() throws Exception {
        MutableWorker worker = new MutableWorker();
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> worker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            worker.alive = false;
            pool.returnWorker(borrowed);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(1L, statistics.getHealthCheckFailureCount());
            assertEquals(1L, statistics.getDiscardCount());
            assertEquals(0, statistics.getIdleWorkers());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should discard returned worker after pool shutdown")
    void shouldDiscardReturnedWorkerAfterPoolShutdown() throws Exception {
        MutableWorker worker = new MutableWorker();
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> worker
        );

        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();
            pool.shutdown();
            pool.returnWorker(borrowed);

            ExternalProcessWorkerPoolStatistics statistics = pool.getStatistics();
            assertEquals(1L, statistics.getDiscardCount());
            assertEquals(0, statistics.getIdleWorkers());
            assertEquals(0, statistics.getCreatedWorkers());
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should reject borrow after pool shutdown")
    void shouldRejectBorrowAfterPoolShutdown() {
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> new MutableWorker()
        );

        pool.shutdown();
        assertThrows(IllegalStateException.class, pool::borrowWorker);
    }

    @Test
    @DisplayName("Should time out when borrow exceeds configured timeout")
    void shouldTimeOutWhenBorrowExceedsConfiguredTimeout() {
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                10L,
                (javaCommand, classpath) -> new ExternalProcessWorkerClient() {
                    @Override
                    public boolean isAlive() {
                        return false;
                    }

                    @Override
                    public synchronized boolean ping() {
                        return false;
                    }
                }
        );

        try {
            assertTimeoutPreemptively(java.time.Duration.ofSeconds(1), () -> {
                assertThrows(ExternalProcessWorkerBorrowTimeoutException.class, pool::borrowWorker);
            });
        } finally {
            pool.shutdown();
        }
    }

    @Test
    @DisplayName("Should unblock waiting borrow after pool shutdown")
    void shouldUnblockWaitingBorrowAfterPoolShutdown() throws Exception {
        MutableWorker worker = new MutableWorker();
        ExternalProcessWorkerPool pool = new ExternalProcessWorkerPool(
                "java",
                "ignored",
                1,
                0,
                0,
                0L,
                (javaCommand, classpath) -> worker
        );

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            ExternalProcessWorkerClient borrowed = pool.borrowWorker();

            Future<?> waiting = executorService.submit(() -> {
                assertThrows(IllegalStateException.class, pool::borrowWorker);
            });

            Thread.sleep(50L);
            pool.shutdown();

            assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
                waiting.get(2, TimeUnit.SECONDS);
            });

            pool.returnWorker(borrowed);
        } finally {
            executorService.shutdownNow();
            pool.shutdown();
        }
    }

    private static final class MutableWorker extends ExternalProcessWorkerClient {

        private boolean alive = true;
        private boolean pingResult = true;
        private final AtomicInteger pingCount = new AtomicInteger(0);
        private ExternalProcessWorkerPool.ProtocolHandshakeSnapshot protocolHandshakeSnapshot =
                ExternalProcessWorkerPool.ProtocolHandshakeSnapshot.empty();

        private MutableWorker() {
            super();
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public synchronized boolean ping() {
            pingCount.incrementAndGet();
            return pingResult;
        }

        @Override
        ExternalProcessWorkerPool.ProtocolHandshakeSnapshot getProtocolHandshakeSnapshot() {
            return protocolHandshakeSnapshot;
        }

        @Override
        public void shutdown() {
            alive = false;
        }
    }
}
