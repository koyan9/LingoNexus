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

import io.github.koyan9.lingonexus.api.lifecycle.LifecycleAware;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple persistent worker pool for external process isolation.
 */
public class ExternalProcessWorkerPool implements LifecycleAware {

    private static final long BORROW_WAIT_TIMEOUT_MS = 200L;

    interface WorkerClientFactory {
        ExternalProcessWorkerClient create(String javaCommand, String classpath) throws IOException;
    }

    private final String javaCommand;
    private final String classpath;
    private final int maxSize;
    private final int startupRetries;
    private final long idleTtlMs;
    private final long borrowTimeoutMs;
    private final WorkerClientFactory workerClientFactory;
    private final BlockingQueue<ExternalProcessWorkerClient> idleWorkers = new LinkedBlockingQueue<ExternalProcessWorkerClient>();
    private final Set<ExternalProcessWorkerClient> allWorkers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger createdWorkers = new AtomicInteger(0);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final AtomicLong borrowCount = new AtomicLong(0);
    private final AtomicLong returnCount = new AtomicLong(0);
    private final AtomicLong discardCount = new AtomicLong(0);
    private final AtomicLong evictionCount = new AtomicLong(0);
    private final AtomicLong startupFailureCount = new AtomicLong(0);
    private final AtomicLong healthCheckFailureCount = new AtomicLong(0);

    public ExternalProcessWorkerPool(String javaCommand, String classpath, int maxSize,
                                     int startupRetries, int prewarmCount, long idleTtlMs) {
        this(javaCommand, classpath, maxSize, startupRetries, prewarmCount, idleTtlMs, 0L, ExternalProcessWorkerClient::new);
    }

    ExternalProcessWorkerPool(String javaCommand, String classpath, int maxSize,
                              int startupRetries, int prewarmCount, long idleTtlMs,
                              WorkerClientFactory workerClientFactory) {
        this(javaCommand, classpath, maxSize, startupRetries, prewarmCount, idleTtlMs, 0L, workerClientFactory);
    }

    public ExternalProcessWorkerPool(String javaCommand, String classpath, int maxSize,
                                     int startupRetries, int prewarmCount, long idleTtlMs,
                                     long borrowTimeoutMs) {
        this(javaCommand, classpath, maxSize, startupRetries, prewarmCount, idleTtlMs, borrowTimeoutMs, ExternalProcessWorkerClient::new);
    }

    ExternalProcessWorkerPool(String javaCommand, String classpath, int maxSize,
                              int startupRetries, int prewarmCount, long idleTtlMs,
                              long borrowTimeoutMs, WorkerClientFactory workerClientFactory) {
        this.javaCommand = javaCommand;
        this.classpath = classpath;
        this.maxSize = Math.max(1, maxSize);
        this.startupRetries = Math.max(0, startupRetries);
        this.idleTtlMs = Math.max(0L, idleTtlMs);
        this.borrowTimeoutMs = Math.max(0L, borrowTimeoutMs);
        this.workerClientFactory = workerClientFactory;
        prewarm(prewarmCount);
    }

    public ExternalProcessWorkerClient borrowWorker() throws IOException, InterruptedException {
        return borrowWorkerWithTimeout(borrowTimeoutMs);
    }

    public void returnWorker(ExternalProcessWorkerClient worker) {
        if (worker == null) {
            return;
        }
        if (isShutdown()) {
            discardWorker(worker);
            return;
        }
        if (!worker.isAlive()) {
            healthCheckFailureCount.incrementAndGet();
            discardWorker(worker);
            return;
        }
        worker.markReturned();
        returnCount.incrementAndGet();
        idleWorkers.offer(worker);
    }

    public void prewarm(int count) {
        int target = Math.min(maxSize, Math.max(0, count));
        for (int i = 0; i < target; i++) {
            try {
                ExternalProcessWorkerClient worker = borrowWorker();
                returnWorker(worker);
            } catch (Exception ignored) {
                break;
            }
        }
    }

    public void discardWorker(ExternalProcessWorkerClient worker) {
        if (worker == null) {
            return;
        }
        worker.shutdown();
        allWorkers.remove(worker);
        discardCount.incrementAndGet();
        createdWorkers.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    public ExternalProcessWorkerPoolStatistics getStatistics() {
        return new ExternalProcessWorkerPoolStatistics(
                maxSize,
                createdWorkers.get(),
                idleWorkers.size(),
                borrowCount.get(),
                returnCount.get(),
                discardCount.get(),
                evictionCount.get(),
                startupFailureCount.get(),
                healthCheckFailureCount.get()
        );
    }


    public ProtocolHandshakeSnapshot getProtocolHandshakeSnapshot() {
        for (ExternalProcessWorkerClient worker : allWorkers) {
            if (worker == null) {
                continue;
            }
            ProtocolHandshakeSnapshot snapshot = worker.getProtocolHandshakeSnapshot();
            if (snapshot != null && !snapshot.isEmpty()) {
                return snapshot;
            }
        }
        return ProtocolHandshakeSnapshot.empty();
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        for (ExternalProcessWorkerClient worker : allWorkers) {
            worker.shutdown();
        }
        idleWorkers.clear();
        allWorkers.clear();
        createdWorkers.set(0);
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get();
    }

    private void ensureActive() {
        if (isShutdown()) {
            throw new IllegalStateException("External process worker pool has been shut down");
        }
    }

    private ExternalProcessWorkerClient borrowWorkerWithTimeout(long timeoutMs) throws IOException, InterruptedException {
        ensureActive();
        long deadlineNanos = timeoutMs > 0 ? System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs) : Long.MAX_VALUE;

        while (true) {
            if (isShutdown()) {
                throw new IllegalStateException("External process worker pool has been shut down");
            }
            if (timeoutMs > 0 && System.nanoTime() > deadlineNanos) {
                throw new ExternalProcessWorkerBorrowTimeoutException(
                        "External process worker borrow timed out after " + timeoutMs + "ms"
                );
            }
            ExternalProcessWorkerClient worker = idleWorkers.poll();
            if (worker != null) {
                if (worker.isIdleExpired(idleTtlMs)) {
                    evictionCount.incrementAndGet();
                    discardWorker(worker);
                    continue;
                }
                if (worker.isAlive() && worker.ping()) {
                    worker.markBorrowed();
                    borrowCount.incrementAndGet();
                    return worker;
                }
                healthCheckFailureCount.incrementAndGet();
                discardWorker(worker);
                continue;
            }

            if (createdWorkers.get() < maxSize) {
                synchronized (this) {
                    if (createdWorkers.get() < maxSize) {
                        createdWorkers.incrementAndGet();
                        try {
                            ExternalProcessWorkerClient newWorker = createWorkerWithRetry();
                            if (newWorker.isAlive() && newWorker.ping()) {
                                allWorkers.add(newWorker);
                                newWorker.markBorrowed();
                                borrowCount.incrementAndGet();
                                return newWorker;
                            }
                            healthCheckFailureCount.incrementAndGet();
                            newWorker.shutdown();
                            createdWorkers.decrementAndGet();
                            continue;
                        } catch (IOException e) {
                            createdWorkers.decrementAndGet();
                            throw e;
                        }
                    }
                }
            }

            long waitMs = resolveBorrowWaitMs(timeoutMs, deadlineNanos);
            if (waitMs <= 0L) {
                throw new ExternalProcessWorkerBorrowTimeoutException(
                        "External process worker borrow timed out after " + timeoutMs + "ms"
                );
            }
            worker = idleWorkers.poll(waitMs, TimeUnit.MILLISECONDS);
            if (worker == null) {
                continue;
            }
            if (worker.isIdleExpired(idleTtlMs)) {
                evictionCount.incrementAndGet();
                discardWorker(worker);
                continue;
            }
            if (worker.isAlive() && worker.ping()) {
                worker.markBorrowed();
                borrowCount.incrementAndGet();
                return worker;
            }
            healthCheckFailureCount.incrementAndGet();
            discardWorker(worker);
        }
    }

    private long resolveBorrowWaitMs(long timeoutMs, long deadlineNanos) {
        if (timeoutMs <= 0L || deadlineNanos == Long.MAX_VALUE) {
            return BORROW_WAIT_TIMEOUT_MS;
        }
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0L) {
            return 0L;
        }
        long remainingMs = TimeUnit.NANOSECONDS.toMillis(remainingNanos);
        if (remainingMs <= 0L) {
            return 0L;
        }
        return Math.min(BORROW_WAIT_TIMEOUT_MS, remainingMs);
    }

    public static final class ProtocolHandshakeSnapshot {
        private static final ProtocolHandshakeSnapshot EMPTY = new ProtocolHandshakeSnapshot(null, Collections.<String>emptyList(), Collections.<String>emptyList());

        private final String protocolVersion;
        private final List<String> supportedTransportProtocolCapabilities;
        private final List<String> supportedTransportSerializerContractIds;

        private ProtocolHandshakeSnapshot(String protocolVersion,
                                          List<String> supportedTransportProtocolCapabilities,
                                          List<String> supportedTransportSerializerContractIds) {
            this.protocolVersion = protocolVersion;
            this.supportedTransportProtocolCapabilities = supportedTransportProtocolCapabilities != null
                    ? supportedTransportProtocolCapabilities
                    : Collections.<String>emptyList();
            this.supportedTransportSerializerContractIds = supportedTransportSerializerContractIds != null
                    ? supportedTransportSerializerContractIds
                    : Collections.<String>emptyList();
        }

        public static ProtocolHandshakeSnapshot empty() {
            return EMPTY;
        }

        static ProtocolHandshakeSnapshot of(String protocolVersion,
                                            List<String> supportedTransportProtocolCapabilities,
                                            List<String> supportedTransportSerializerContractIds) {
            if (protocolVersion == null
                    && (supportedTransportProtocolCapabilities == null || supportedTransportProtocolCapabilities.isEmpty())
                    && (supportedTransportSerializerContractIds == null || supportedTransportSerializerContractIds.isEmpty())) {
                return EMPTY;
            }
            return new ProtocolHandshakeSnapshot(
                    protocolVersion,
                    supportedTransportProtocolCapabilities,
                    supportedTransportSerializerContractIds
            );
        }

        boolean isEmpty() {
            return protocolVersion == null
                    && supportedTransportProtocolCapabilities.isEmpty()
                    && supportedTransportSerializerContractIds.isEmpty();
        }

        public String getProtocolVersion() {
            return protocolVersion;
        }

        public List<String> getSupportedTransportProtocolCapabilities() {
            return supportedTransportProtocolCapabilities;
        }

        public List<String> getSupportedTransportSerializerContractIds() {
            return supportedTransportSerializerContractIds;
        }
    }

    private ExternalProcessWorkerClient createWorkerWithRetry() throws IOException {
        IOException lastException = null;
        for (int attempt = 0; attempt <= startupRetries; attempt++) {
            try {
                return workerClientFactory.create(javaCommand, classpath);
            } catch (IOException e) {
                startupFailureCount.incrementAndGet();
                lastException = e;
            }
        }
        throw lastException != null ? lastException : new IOException("Failed to create external worker");
    }
}
