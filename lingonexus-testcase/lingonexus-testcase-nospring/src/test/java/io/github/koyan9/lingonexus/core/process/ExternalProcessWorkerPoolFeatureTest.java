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
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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
