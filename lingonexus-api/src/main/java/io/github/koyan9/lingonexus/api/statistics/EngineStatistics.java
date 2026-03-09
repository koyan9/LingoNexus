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

import java.util.Map;

/**
 * 引擎统计信息
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class EngineStatistics {

    private final long totalExecutions;
    private final long successfulExecutions;
    private final long failedExecutions;
    private final long cacheHits;
    private final long cacheMisses;
    private final long averageExecutionTimeMs;
    private final Map<String, Long> executionsByLanguage;

    public EngineStatistics(long totalExecutions, long successfulExecutions, long failedExecutions,
                           long cacheHits, long cacheMisses, long averageExecutionTimeMs,
                           Map<String, Long> executionsByLanguage) {
        this.totalExecutions = totalExecutions;
        this.successfulExecutions = successfulExecutions;
        this.failedExecutions = failedExecutions;
        this.cacheHits = cacheHits;
        this.cacheMisses = cacheMisses;
        this.averageExecutionTimeMs = averageExecutionTimeMs;
        this.executionsByLanguage = executionsByLanguage;
    }

    public long getTotalExecutions() {
        return totalExecutions;
    }

    public long getSuccessfulExecutions() {
        return successfulExecutions;
    }

    public long getFailedExecutions() {
        return failedExecutions;
    }

    public long getCacheHits() {
        return cacheHits;
    }

    public long getCacheMisses() {
        return cacheMisses;
    }

    public double getCacheHitRate() {
        if (cacheHits + cacheMisses == 0) {
            return 0.0;
        }
        return (double) cacheHits / (cacheHits + cacheMisses);
    }

    public long getAverageExecutionTimeMs() {
        return averageExecutionTimeMs;
    }

    public Map<String, Long> getExecutionsByLanguage() {
        return executionsByLanguage;
    }

    public double getSuccessRate() {
        if (totalExecutions == 0) {
            return 0.0;
        }
        return (double) successfulExecutions / totalExecutions;
    }
}
