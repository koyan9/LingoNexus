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

/**
 * Aggregated diagnostics for engine runtime state.
 */
public class EngineDiagnostics {

    private final EngineStatistics statistics;
    private final long cacheSize;
    private final String asyncExecutorStatistics;
    private final String isolatedExecutorStatistics;
    private final ExternalProcessStatistics externalProcessStatistics;
    private final String isolationMode;
    private final boolean shutdown;

    public EngineDiagnostics(EngineStatistics statistics, long cacheSize,
                             String asyncExecutorStatistics, String isolatedExecutorStatistics,
                             ExternalProcessStatistics externalProcessStatistics,
                             String isolationMode, boolean shutdown) {
        this.statistics = statistics;
        this.cacheSize = cacheSize;
        this.asyncExecutorStatistics = asyncExecutorStatistics;
        this.isolatedExecutorStatistics = isolatedExecutorStatistics;
        this.externalProcessStatistics = externalProcessStatistics;
        this.isolationMode = isolationMode;
        this.shutdown = shutdown;
    }

    public EngineStatistics getStatistics() {
        return statistics;
    }

    public long getCacheSize() {
        return cacheSize;
    }

    public String getAsyncExecutorStatistics() {
        return asyncExecutorStatistics;
    }

    public String getIsolatedExecutorStatistics() {
        return isolatedExecutorStatistics;
    }

    public ExternalProcessStatistics getExternalProcessStatistics() {
        return externalProcessStatistics;
    }

    public String getIsolationMode() {
        return isolationMode;
    }

    public boolean isShutdown() {
        return shutdown;
    }
}
