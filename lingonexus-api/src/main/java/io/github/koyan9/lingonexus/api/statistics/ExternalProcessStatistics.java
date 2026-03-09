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
package io.github.koyan9.lingonexus.api.statistics;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * External-process worker statistics snapshot.
 */
public class ExternalProcessStatistics {

    private final int maxWorkers;
    private final int createdWorkers;
    private final int idleWorkers;
    private final long borrowCount;
    private final long returnCount;
    private final long discardCount;
    private final long evictionCount;
    private final long startupFailureCount;
    private final long healthCheckFailureCount;
    private final long executorCacheSize;
    private final long executorCacheHits;
    private final long executorCacheMisses;
    private final long executorCacheEvictions;
    private final String workerProtocolVersion;
    private final List<String> supportedTransportProtocolCapabilities;
    private final List<String> supportedTransportSerializerContractIds;
    private final long protocolNegotiationFailureCount;
    private final String latestProtocolNegotiationFailureReason;
    private final String latestBorrowFailureReason;
    private final String latestWorkerExecutionFailureReason;
    private final Map<String, Long> failureReasonCounts;

    public ExternalProcessStatistics(int maxWorkers, int createdWorkers, int idleWorkers,
                                     long borrowCount, long returnCount, long discardCount,
                                     long evictionCount, long startupFailureCount,
                                     long healthCheckFailureCount,
                                     long executorCacheSize, long executorCacheHits,
                                     long executorCacheMisses, long executorCacheEvictions,
                                     String workerProtocolVersion,
                                     List<String> supportedTransportProtocolCapabilities,
                                     List<String> supportedTransportSerializerContractIds,
                                     long protocolNegotiationFailureCount,
                                     String latestProtocolNegotiationFailureReason,
                                     String latestBorrowFailureReason,
                                     String latestWorkerExecutionFailureReason,
                                     Map<String, Long> failureReasonCounts) {
        this.maxWorkers = maxWorkers;
        this.createdWorkers = createdWorkers;
        this.idleWorkers = idleWorkers;
        this.borrowCount = borrowCount;
        this.returnCount = returnCount;
        this.discardCount = discardCount;
        this.evictionCount = evictionCount;
        this.startupFailureCount = startupFailureCount;
        this.healthCheckFailureCount = healthCheckFailureCount;
        this.executorCacheSize = executorCacheSize;
        this.executorCacheHits = executorCacheHits;
        this.executorCacheMisses = executorCacheMisses;
        this.executorCacheEvictions = executorCacheEvictions;
        this.workerProtocolVersion = workerProtocolVersion;
        this.supportedTransportProtocolCapabilities = supportedTransportProtocolCapabilities != null
                ? Collections.unmodifiableList(supportedTransportProtocolCapabilities)
                : Collections.<String>emptyList();
        this.supportedTransportSerializerContractIds = supportedTransportSerializerContractIds != null
                ? Collections.unmodifiableList(supportedTransportSerializerContractIds)
                : Collections.<String>emptyList();
        this.protocolNegotiationFailureCount = protocolNegotiationFailureCount;
        this.latestProtocolNegotiationFailureReason = latestProtocolNegotiationFailureReason;
        this.latestBorrowFailureReason = latestBorrowFailureReason;
        this.latestWorkerExecutionFailureReason = latestWorkerExecutionFailureReason;
        this.failureReasonCounts = failureReasonCounts != null
                ? Collections.unmodifiableMap(new LinkedHashMap<String, Long>(failureReasonCounts))
                : Collections.<String, Long>emptyMap();
    }

    public int getMaxWorkers() {
        return maxWorkers;
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

    public long getEvictionCount() {
        return evictionCount;
    }

    public long getStartupFailureCount() {
        return startupFailureCount;
    }

    public long getHealthCheckFailureCount() {
        return healthCheckFailureCount;
    }

    public long getExecutorCacheSize() {
        return executorCacheSize;
    }

    public long getExecutorCacheHits() {
        return executorCacheHits;
    }

    public long getExecutorCacheMisses() {
        return executorCacheMisses;
    }

    public long getExecutorCacheEvictions() {
        return executorCacheEvictions;
    }


    public String getWorkerProtocolVersion() {
        return workerProtocolVersion;
    }

    public List<String> getSupportedTransportProtocolCapabilities() {
        return supportedTransportProtocolCapabilities;
    }

    public List<String> getSupportedTransportSerializerContractIds() {
        return supportedTransportSerializerContractIds;
    }


    public long getProtocolNegotiationFailureCount() {
        return protocolNegotiationFailureCount;
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
        return failureReasonCounts;
    }
}
