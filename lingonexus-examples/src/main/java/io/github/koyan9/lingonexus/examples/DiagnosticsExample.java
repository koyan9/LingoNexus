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
package io.github.koyan9.lingonexus.examples;

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import io.github.koyan9.lingonexus.api.statistics.EngineDiagnostics;
import io.github.koyan9.lingonexus.api.statistics.EngineStatistics;
import io.github.koyan9.lingonexus.core.LingoNexusBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Demonstrates runtime diagnostics and worker-pool inspection.
 */
public class DiagnosticsExample {

    public static void main(String[] args) {
        LingoNexusConfig config = LingoNexusConfig.builder()
                .defaultLanguage(ScriptLanguage.GROOVY)
                .sandboxConfig(SandboxConfig.builder()
                        .enabled(true)
                        .timeoutMs(2000)
                        .isolationMode(ExecutionIsolationMode.EXTERNAL_PROCESS)
                        .externalProcessPoolSize(2)
                        .externalProcessPrewarmCount(1)
                        .externalProcessStartupRetries(2)
                        .build())
                .build();

        LingoNexusExecutor executor = LingoNexusBuilder.createNewInstance(config);
        try {
            Map<String, Object> vars = new HashMap<String, Object>();
            vars.put("value", 21);

            ScriptResult result1 = executor.execute("return value * 2", "groovy", ScriptContext.of(vars));
            ScriptResult result2 = executor.execute("return 100 + 23", "groovy", ScriptContext.of(Collections.emptyMap()));

            System.out.println("=== Diagnostics Example ===");
            System.out.println("Result 1: " + result1.getValue());
            System.out.println("Result 2: " + result2.getValue());

            EngineStatistics statistics = executor.getStatistics();
            EngineDiagnostics diagnostics = executor.getDiagnostics();

            System.out.println("\n--- Engine Statistics ---");
            System.out.println("Total Executions: " + statistics.getTotalExecutions());
            System.out.println("Successful Executions: " + statistics.getSuccessfulExecutions());
            System.out.println("Failed Executions: " + statistics.getFailedExecutions());
            System.out.println("Average Execution Time (ms): " + statistics.getAverageExecutionTimeMs());
            System.out.println("Executions By Language: " + statistics.getExecutionsByLanguage());

            System.out.println("\n--- Runtime Diagnostics ---");
            System.out.println("Isolation Mode: " + diagnostics.getIsolationMode());
            System.out.println("Cache Size: " + diagnostics.getCacheSize());
            System.out.println("Async Pool: " + diagnostics.getAsyncExecutorStatistics());
            System.out.println("Isolated Pool: " + diagnostics.getIsolatedExecutorStatistics());

            if (diagnostics.getExternalProcessStatistics() != null) {
                System.out.println("\n--- External Worker Pool ---");
                System.out.println("Max Workers: " + diagnostics.getExternalProcessStatistics().getMaxWorkers());
                System.out.println("Created Workers: " + diagnostics.getExternalProcessStatistics().getCreatedWorkers());
                System.out.println("Idle Workers: " + diagnostics.getExternalProcessStatistics().getIdleWorkers());
                System.out.println("Borrow Count: " + diagnostics.getExternalProcessStatistics().getBorrowCount());
                System.out.println("Return Count: " + diagnostics.getExternalProcessStatistics().getReturnCount());
                System.out.println("Discard Count: " + diagnostics.getExternalProcessStatistics().getDiscardCount());
                System.out.println("Eviction Count: " + diagnostics.getExternalProcessStatistics().getEvictionCount());
                System.out.println("Startup Failure Count: " + diagnostics.getExternalProcessStatistics().getStartupFailureCount());
                System.out.println("Health Check Failure Count: " + diagnostics.getExternalProcessStatistics().getHealthCheckFailureCount());
                System.out.println("Executor Cache Size: " + diagnostics.getExternalProcessStatistics().getExecutorCacheSize());
                System.out.println("Executor Cache Hits: " + diagnostics.getExternalProcessStatistics().getExecutorCacheHits());
                System.out.println("Executor Cache Misses: " + diagnostics.getExternalProcessStatistics().getExecutorCacheMisses());
                System.out.println("Executor Cache Evictions: " + diagnostics.getExternalProcessStatistics().getExecutorCacheEvictions());
            }
        } finally {
            executor.close();
        }
    }
}
