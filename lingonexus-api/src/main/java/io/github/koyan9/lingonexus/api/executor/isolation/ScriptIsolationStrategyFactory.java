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
package io.github.koyan9.lingonexus.api.executor.isolation;

import io.github.koyan9.lingonexus.api.config.EngineConfig;

/**
 * Creates isolation strategies based on engine and sandbox configuration.
 */
public final class ScriptIsolationStrategyFactory {

    private ScriptIsolationStrategyFactory() {
    }

    public static ScriptIsolationStrategy create(EngineConfig engineConfig) {
        ExecutionIsolationMode mode = engineConfig.getSandboxConfig().getIsolationMode();
        switch (mode) {
            case DIRECT:
                return new DirectIsolationStrategy();
            case ISOLATED_THREAD:
                return new ThreadPoolIsolationStrategy(
                        engineConfig.getThreadPoolManager().getSharedIsolatedExecutor(engineConfig.getIsolatedExecutorConfig())
                );
            case EXTERNAL_PROCESS:
                return new ExternalProcessIsolationStrategy();
            case AUTO:
            default:
                if (engineConfig.getSandboxConfig().getTimeoutMs() > 0) {
                    return new ThreadPoolIsolationStrategy(
                            engineConfig.getThreadPoolManager().getSharedIsolatedExecutor(engineConfig.getIsolatedExecutorConfig())
                    );
                }
                return new DirectIsolationStrategy();
        }
    }
}
