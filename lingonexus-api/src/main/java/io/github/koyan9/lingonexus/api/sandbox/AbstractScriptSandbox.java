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
package io.github.koyan9.lingonexus.api.sandbox;

import io.github.koyan9.lingonexus.api.config.EngineConfig;
import io.github.koyan9.lingonexus.api.config.SandboxConfig;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants.ResultMetadataKeys;
import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.executor.IsolatedExecutionResult;
import io.github.koyan9.lingonexus.api.executor.isolation.ScriptIsolationStrategy;
import io.github.koyan9.lingonexus.api.executor.isolation.ScriptIsolationStrategyFactory;
import io.github.koyan9.lingonexus.api.sandbox.spi.ScriptSandbox;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 脚本沙箱抽象基类，扩展类统一继承该抽象类。
 */
public abstract class AbstractScriptSandbox implements ScriptSandbox {

    protected final EngineConfig engineConfig;
    protected final SandboxConfig sandboxConfig;

    private final ScriptIsolationStrategy isolationStrategy;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public AbstractScriptSandbox(EngineConfig engineConfig) {
        this.engineConfig = engineConfig;
        this.sandboxConfig = engineConfig.getSandboxConfig();
        this.isolationStrategy = ScriptIsolationStrategyFactory.create(engineConfig);
    }

    @Override
    public SandboxConfig getConfig() {
        return sandboxConfig;
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }

        try {
            onShutdown();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to shutdown sandbox: " + getClass().getSimpleName(), e);
        } finally {
            isolationStrategy.shutdown();
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown.get() || isolationStrategy.isShutdown();
    }

    protected final <T> T executeWithIsolation(ScriptContext context, Callable<T> task, ClassLoader contextClassLoader) throws Exception {
        ensureActive();
        IsolatedExecutionResult<T> executionResult = isolationStrategy.execute(task, contextClassLoader, sandboxConfig.getTimeoutMs());
        recordExecutionMetrics(context, executionResult);
        return executionResult.getValue();
    }

    protected final <T> T executeWithIsolation(ScriptContext context, Callable<T> task) throws Exception {
        return executeWithIsolation(context, task, null);
    }

    protected final void ensureActive() {
        if (isShutdown()) {
            throw new IllegalStateException(getClass().getSimpleName() + " has been shut down");
        }
    }

    protected void onShutdown() throws Exception {
    }

    private void recordExecutionMetrics(ScriptContext context, IsolatedExecutionResult<?> executionResult) {
        if (context == null || executionResult == null) {
            return;
        }

        context.setMetadata(ResultMetadataKeys.QUEUE_WAIT_TIME, executionResult.getQueueWaitTimeMs());
        context.setMetadata(ResultMetadataKeys.EXECUTION_TIME, executionResult.getExecutionTimeMs());
        context.setMetadata(ResultMetadataKeys.WALL_TIME, executionResult.getWallTimeMs());
    }
}
