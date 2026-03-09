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

import io.github.koyan9.lingonexus.api.executor.IsolatedExecutionResult;
import io.github.koyan9.lingonexus.api.executor.IsolatedScriptExecutor;

import java.util.concurrent.Callable;

/**
 * Executes scripts in the isolated thread pool.
 */
public class ThreadPoolIsolationStrategy implements ScriptIsolationStrategy {

    private final IsolatedScriptExecutor isolatedExecutor;

    public ThreadPoolIsolationStrategy(IsolatedScriptExecutor isolatedExecutor) {
        this.isolatedExecutor = isolatedExecutor;
    }

    @Override
    public <T> IsolatedExecutionResult<T> execute(Callable<T> task, ClassLoader contextClassLoader, long timeoutMs) throws Exception {
        if (timeoutMs > 0) {
            return isolatedExecutor.executeWithMetrics(task, contextClassLoader, timeoutMs);
        }
        return isolatedExecutor.executeWithMetrics(task, contextClassLoader);
    }

    @Override
    public ExecutionIsolationMode getMode() {
        return ExecutionIsolationMode.ISOLATED_THREAD;
    }

    @Override
    public boolean isShutdown() {
        return isolatedExecutor.isShutdown();
    }
}
