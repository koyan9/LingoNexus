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
package io.github.koyan9.lingonexus.core.executor;

import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/**
 * Concurrent execution statistics collector for the engine hot path.
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-06
 */
public class ExecutionStatisticsCollector {

    private final LongAdder totalExecutions = new LongAdder();
    private final LongAdder successfulExecutions = new LongAdder();
    private final LongAdder failedExecutions = new LongAdder();
    private final LongAdder cacheHits = new LongAdder();
    private final LongAdder cacheMisses = new LongAdder();
    private final LongAdder totalExecutionTimeMs = new LongAdder();
    private final ConcurrentHashMap<String, LongAdder> executionsByLanguage = new ConcurrentHashMap<String, LongAdder>();

    public void record(String language, boolean success, Boolean cacheHit, long executionTimeMs) {
        totalExecutions.increment();
        if (success) {
            successfulExecutions.increment();
        } else {
            failedExecutions.increment();
        }

        if (cacheHit != null) {
            if (cacheHit.booleanValue()) {
                cacheHits.increment();
            } else {
                cacheMisses.increment();
            }
        }

        totalExecutionTimeMs.add(Math.max(0L, executionTimeMs));

        String languageKey = language != null ? language.toLowerCase() : "unknown";
        executionsByLanguage.computeIfAbsent(languageKey, key -> new LongAdder()).increment();
    }

    public EngineStatistics snapshot() {
        long total = totalExecutions.sum();
        long averageExecutionTime = total == 0L ? 0L : totalExecutionTimeMs.sum() / total;

        Map<String, Long> executionSnapshot = new HashMap<String, Long>();
        for (Map.Entry<String, LongAdder> entry : executionsByLanguage.entrySet()) {
            executionSnapshot.put(entry.getKey(), entry.getValue().sum());
        }

        return new EngineStatistics(
                total,
                successfulExecutions.sum(),
                failedExecutions.sum(),
                cacheHits.sum(),
                cacheMisses.sum(),
                averageExecutionTime,
                executionSnapshot
        );
    }
}