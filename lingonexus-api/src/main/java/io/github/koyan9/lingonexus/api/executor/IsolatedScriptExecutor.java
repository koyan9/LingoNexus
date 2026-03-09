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

package io.github.koyan9.lingonexus.api.executor;

import io.github.koyan9.lingonexus.api.exception.ScriptRuntimeException;
import io.github.koyan9.lingonexus.api.exception.ScriptTimeoutException;
import io.github.koyan9.lingonexus.api.security.ClassLoaderScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Isolated script executor with thread pool and RestrictedClassLoader
 *
 * <p>This executor provides isolated thread execution for scripts with the following features:</p>
 * <ul>
 *   <li>🧵 Thread isolation: Each script runs in a dedicated thread from the pool</li>
 *   <li>🔒 No thread pollution: Main thread ClassLoader remains unchanged</li>
 *   <li>🛡️ Runtime security: RestrictedClassLoader enforced at runtime</li>
 *   <li>⚡ Thread pool: Efficient thread reuse managed by ThreadPoolManager</li>
 *   <li>✅ Exception-safe: Proper cleanup guaranteed</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Thread pool is now managed by ThreadPoolManager for centralized management.</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-10
 */
public class IsolatedScriptExecutor {

    private static final Logger logger = LoggerFactory.getLogger(IsolatedScriptExecutor.class);

    /**
     * Thread pool executor (managed by ThreadPoolManager)
     */
    private final ThreadPoolExecutor executor;

    /**
     * Constructor with ThreadPoolExecutor
     *
     * @param executor Thread pool executor from ThreadPoolManager
     */
    public IsolatedScriptExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
        logger.info("IsolatedScriptExecutor initialized with thread pool from ThreadPoolManager");
    }

    /**
     * Execute script task in isolated thread with RestrictedClassLoader and timeout
     *
     * @param task                  脚本执行任务
     * @param restrictedClassLoader 受限类加载器（可选，如果为 null 则不设置）
     * @param timeoutMs             超时时间（毫秒）
     * @param <T>                   返回值类型
     * @return 执行结果
     * @throws ScriptRuntimeException 执行失败或超时
     */
    public <T> T executeInIsolatedThread(Callable<T> task, ClassLoader contextClassLoader, long timeoutMs) {
        return executeWithMetrics(() -> ClassLoaderScope.call(contextClassLoader, task), timeoutMs).getValue();
    }

    public <T> IsolatedExecutionResult<T> executeWithMetrics(Callable<T> task, long timeoutMs) {
        final long submissionStartNanos = System.nanoTime();
        Future<IsolatedExecutionResult<T>> future = executor.submit(() -> {
            long executionStartNanos = System.nanoTime();
            long queueWaitTimeMs = nanosToMillis(executionStartNanos - submissionStartNanos);
            T value = task.call();
            long completionNanos = System.nanoTime();
            long executionTimeMs = nanosToMillis(completionNanos - executionStartNanos);
            long wallTimeMs = nanosToMillis(completionNanos - submissionStartNanos);

            return new IsolatedExecutionResult<T>(value, queueWaitTimeMs, executionTimeMs, wallTimeMs);
        });

        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            long actualExecutionTimeMs = nanosToMillis(System.nanoTime() - submissionStartNanos);
            throw new ScriptTimeoutException(timeoutMs, actualExecutionTimeMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptRuntimeException("Script execution interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ScriptRuntimeException("Script execution failed: " + cause.getMessage(), cause);
        }
    }

    /**
     * Execute script task in isolated thread with RestrictedClassLoader
     * <p>This method uses a default timeout of 5 minutes (300000ms)</p>
     *
     * @param task                  脚本执行任务
     * @param restrictedClassLoader 受限类加载器（可选，如果为 null 则不设置）
     * @param <T>                   返回值类型
     * @return 执行结果
     * @throws ScriptRuntimeException 执行失败
     */
    public <T> T executeInIsolatedThread(Callable<T> task, ClassLoader contextClassLoader) {
        // Use default timeout of 5 minutes
        return executeInIsolatedThread(task, contextClassLoader, 300000L);
    }

    public <T> IsolatedExecutionResult<T> executeWithMetrics(Callable<T> task, ClassLoader contextClassLoader, long timeoutMs) {
        return executeWithMetrics(() -> ClassLoaderScope.call(contextClassLoader, task), timeoutMs);
    }

    public <T> IsolatedExecutionResult<T> executeWithMetrics(Callable<T> task) {
        final long submissionStartNanos = System.nanoTime();
        Future<IsolatedExecutionResult<T>> future = executor.submit(() -> {
            long executionStartNanos = System.nanoTime();
            long queueWaitTimeMs = nanosToMillis(executionStartNanos - submissionStartNanos);
            T value = task.call();
            long completionNanos = System.nanoTime();
            long executionTimeMs = nanosToMillis(completionNanos - executionStartNanos);
            long wallTimeMs = nanosToMillis(completionNanos - submissionStartNanos);

            return new IsolatedExecutionResult<T>(value, queueWaitTimeMs, executionTimeMs, wallTimeMs);
        });

        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScriptRuntimeException("Script execution interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new ScriptRuntimeException("Script execution failed: " + cause.getMessage(), cause);
        }
    }

    public <T> IsolatedExecutionResult<T> executeWithMetrics(Callable<T> task, ClassLoader contextClassLoader) {
        return executeWithMetrics(() -> ClassLoaderScope.call(contextClassLoader, task));
    }

    /**
     * Execute script task in isolated thread without RestrictedClassLoader
     *
     * @param task 脚本执行任务
     * @param <T>  返回值类型
     * @return 执行结果
     */
    public <T> T executeInIsolatedThread(Callable<T> task) {
        return executeWithMetrics(task).getValue();
    }

    /**
     * Get thread pool statistics
     *
     * @return 线程池统计信息
     */
    public String getStatistics() {
        return String.format(
                "ThreadPool[active=%d, poolSize=%d, corePoolSize=%d, maxPoolSize=%d, queueSize=%d, completedTasks=%d]",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

    public boolean isShutdown() {
        return executor.isShutdown() || executor.isTerminated();
    }

    private long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }
}
