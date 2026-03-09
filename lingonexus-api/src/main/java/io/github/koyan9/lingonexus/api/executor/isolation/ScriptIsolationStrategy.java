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
import io.github.koyan9.lingonexus.api.lifecycle.LifecycleAware;

import java.util.concurrent.Callable;

/**
 * Strategy for executing scripts under a specific isolation mode.
 */
public interface ScriptIsolationStrategy extends LifecycleAware {

    /**
     * Execute task under the configured isolation model.
     *
     * @param task task to execute
     * @param contextClassLoader optional context classloader
     * @param timeoutMs timeout in milliseconds, {@code <= 0} means no timeout enforcement
     * @param <T> result type
     * @return execution result with timing metadata
     * @throws Exception task failure or isolation failure
     */
    <T> IsolatedExecutionResult<T> execute(Callable<T> task, ClassLoader contextClassLoader, long timeoutMs) throws Exception;

    /**
     * @return isolation mode handled by this strategy
     */
    ExecutionIsolationMode getMode();
}
