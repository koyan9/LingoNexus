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

/**
 * Snapshot of external worker pool statistics.
 */
public class ExternalProcessWorkerPoolStatistics {

    private final int maxSize;
    private final int createdWorkers;
    private final int idleWorkers;
    private final long borrowCount;
    private final long returnCount;
    private final long discardCount;
    private final long borrowTimeoutCount;
    private final long evictionCount;
    private final long startupFailureCount;
    private final long healthCheckFailureCount;

    public ExternalProcessWorkerPoolStatistics(int maxSize, int createdWorkers, int idleWorkers,
                                               long borrowCount, long returnCount, long discardCount,
                                               long borrowTimeoutCount,
                                               long evictionCount,
                                               long startupFailureCount, long healthCheckFailureCount) {
        this.maxSize = maxSize;
        this.createdWorkers = createdWorkers;
        this.idleWorkers = idleWorkers;
        this.borrowCount = borrowCount;
        this.returnCount = returnCount;
        this.discardCount = discardCount;
        this.borrowTimeoutCount = borrowTimeoutCount;
        this.evictionCount = evictionCount;
        this.startupFailureCount = startupFailureCount;
        this.healthCheckFailureCount = healthCheckFailureCount;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public int getCreatedWorkers() {
        return createdWorkers;
    }

    public int getIdleWorkers() {
        return idleWorkers;
    }

    public long getBorrowCount() {
        return borrowCount;
    }

    public long getReturnCount() {
        return returnCount;
    }

    public long getDiscardCount() {
        return discardCount;
    }

    public long getBorrowTimeoutCount() {
        return borrowTimeoutCount;
    }

    public long getEvictionCount() {
        return evictionCount;
    }

    public long getStartupFailureCount() {
        return startupFailureCount;
    }

    public long getHealthCheckFailureCount() {
        return healthCheckFailureCount;
    }

    @Override
    public String toString() {
        return "ExternalProcessWorkerPoolStatistics{" +
                "maxSize=" + maxSize +
                ", createdWorkers=" + createdWorkers +
                ", idleWorkers=" + idleWorkers +
                ", borrowCount=" + borrowCount +
                ", returnCount=" + returnCount +
                ", discardCount=" + discardCount +
                ", borrowTimeoutCount=" + borrowTimeoutCount +
                ", evictionCount=" + evictionCount +
                ", startupFailureCount=" + startupFailureCount +
                ", healthCheckFailureCount=" + healthCheckFailureCount +
                '}';
    }
}
