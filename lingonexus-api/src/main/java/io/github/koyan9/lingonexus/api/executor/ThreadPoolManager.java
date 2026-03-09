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

import io.github.koyan9.lingonexus.api.config.ExecutorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unified thread pool manager for LingoNexus
 *
 * <p>This manager provides centralized thread pool management with the following features:</p>
 * <ul>
 *   <li>🎯 Centralized management: All thread pools managed in one place</li>
 *   <li>⚡ Lazy initialization: Thread pools created on demand</li>
 *   <li>🔧 Configurable: Support custom ExecutorConfig for each pool</li>
 *   <li>🛡️ Thread-safe: Concurrent access protected</li>
 *   <li>♻️ Lifecycle management: Graceful shutdown support</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-25
 */
public class ThreadPoolManager {

    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolManager.class);

    /**
     * Thread pool type enum
     */
    public enum PoolType {
        /**
         * Async executor pool for DefaultScriptExecutor
         */
        ASYNC_EXECUTOR("ScriptExecutor-Async-"),

        /**
         * Isolated executor pool for IsolatedScriptExecutor
         */
        ISOLATED_EXECUTOR("ScriptExecutor-Isolated-");

        private final String threadNamePrefix;

        PoolType(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
    }

    /**
     * Singleton instance
     */
    private static volatile ThreadPoolManager instance;

    /**
     * Thread pool storage
     */
    private final Map<PoolType, ThreadPoolExecutor> executorMap = new ConcurrentHashMap<>();

    /**
     * Configuration storage
     */
    private final Map<PoolType, ExecutorConfig> configMap = new ConcurrentHashMap<>();

    /**
     * Thread counter for each pool type
     */
    private final Map<PoolType, AtomicInteger> threadCounterMap = new ConcurrentHashMap<>();

    /**
     * Shared IsolatedScriptExecutor instance
     */
    private volatile IsolatedScriptExecutor sharedIsolatedExecutor;

    /**
     * Private constructor for singleton
     */
    private ThreadPoolManager() {
        logger.info("ThreadPoolManager initialized");
    }

    /**
     * Get singleton instance
     *
     * @return ThreadPoolManager instance
     */
    public static ThreadPoolManager getInstance() {
        if (instance == null) {
            synchronized (ThreadPoolManager.class) {
                if (instance == null) {
                    instance = new ThreadPoolManager();
                }
            }
        }
        return instance;
    }

    public static ThreadPoolManager createIndependentManager() {
        return new ThreadPoolManager();
    }

    /**
     * Get or create thread pool executor for specified type
     *
     * @param poolType Pool type
     * @return ThreadPoolExecutor instance
     */
    public ThreadPoolExecutor getExecutor(PoolType poolType) {
        return this.getExecutor(poolType, configMap.get(poolType));
    }

    /**
     * Get or create thread pool executor with custom configuration
     *
     * @param poolType Pool type
     * @param config   Executor configuration
     * @return ThreadPoolExecutor instance
     */
    public ThreadPoolExecutor getExecutor(PoolType poolType, ExecutorConfig config) {
        ExecutorConfig executorConfig = configMap.computeIfAbsent(poolType, type -> {
            if (config != null) {
                ExecutorConfig.Builder builder = ExecutorConfig.builder()
                        .corePoolSize(config.getCorePoolSize())
                        .maxPoolSize(config.getMaxPoolSize())
                        .keepAliveTimeSeconds(config.getKeepAliveTimeSeconds())
                        .queueCapacity(config.getQueueCapacity())
                        .rejectionPolicy(config.getRejectionPolicy());

                // Differentiated thread name prefix handling
                if (type == PoolType.ISOLATED_EXECUTOR) {
                    // ISOLATED_EXECUTOR always uses fixed prefix
                    builder.threadNamePrefix(type.getThreadNamePrefix());
                } else if (type == PoolType.ASYNC_EXECUTOR) {
                    // ASYNC_EXECUTOR: use user's config if provided, otherwise use PoolType's default
                    String userPrefix = config.getThreadNamePrefix();
                    if (userPrefix != null && !userPrefix.isEmpty()) {
                        builder.threadNamePrefix(userPrefix);
                    } else {
                        builder.threadNamePrefix(type.getThreadNamePrefix());
                    }
                }

                return builder.build();
            } else {
                return createDefaultConfig(type);
            }
        });
        return executorMap.computeIfAbsent(poolType, type -> createThreadPoolExecutor(type, executorConfig));
    }

    /**
     * Get shared IsolatedScriptExecutor instance
     * <p>All sandboxes should use this shared instance instead of creating their own</p>
     *
     * @param config Executor configuration (optional, uses default if null)
     * @return Shared IsolatedScriptExecutor instance
     */
    public IsolatedScriptExecutor getSharedIsolatedExecutor(ExecutorConfig config) {
        if (sharedIsolatedExecutor == null || sharedIsolatedExecutor.isShutdown()) {
            synchronized (this) {
                if (sharedIsolatedExecutor == null || sharedIsolatedExecutor.isShutdown()) {
                    ThreadPoolExecutor threadPool = getExecutor(PoolType.ISOLATED_EXECUTOR, config);
                    sharedIsolatedExecutor = new IsolatedScriptExecutor(threadPool);
                }
            }
        }
        return sharedIsolatedExecutor;
    }

    /**
     * Create default configuration for pool type
     */
    private ExecutorConfig createDefaultConfig(PoolType poolType) {
        ExecutorConfig.Builder builder = ExecutorConfig.builder()
                .threadNamePrefix(poolType.getThreadNamePrefix());

        // Different default configurations for different pool types
        switch (poolType) {
            case ASYNC_EXECUTOR:
                // For async execution: moderate pool size
                builder.corePoolSize(Runtime.getRuntime().availableProcessors() * 2)
                        .maxPoolSize(Math.max(Runtime.getRuntime().availableProcessors() * 4, 50))
                        .keepAliveTimeSeconds(60L)
                        .queueCapacity(1000);
                break;
            case ISOLATED_EXECUTOR:
                // For isolated execution: larger pool size for better isolation
                // Increased queue capacity to handle high concurrency scenarios
                builder.corePoolSize(32)
                        .maxPoolSize(256)
                        .keepAliveTimeSeconds(60L)
                        .queueCapacity(2000);  // Increased from 100 to 2000
                break;
        }

        return builder.build();
    }

    /**
     * Create thread pool executor
     */
    private ThreadPoolExecutor createThreadPoolExecutor(PoolType poolType, ExecutorConfig config) {
        AtomicInteger threadCounter = threadCounterMap.computeIfAbsent(poolType, k -> new AtomicInteger(0));

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            thread.setName(config.getThreadNamePrefix() + threadCounter.incrementAndGet());
            return thread;
        };

        // Convert RejectionPolicy enum to ThreadPoolExecutor.RejectedExecutionHandler
        RejectedExecutionHandler rejectedExecutionHandler = getRejectedExecutionHandler(config.getRejectionPolicy());

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                config.getKeepAliveTimeSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(config.getQueueCapacity()),
                threadFactory,
                rejectedExecutionHandler
        );

        logger.info(
                "Created thread pool for {}: corePoolSize={}, maxPoolSize={}, keepAliveTime={}s, queueCapacity={}, threadNamePrefix={}, rejectionPolicy={}",
                poolType,
                config.getCorePoolSize(),
                config.getMaxPoolSize(),
                config.getKeepAliveTimeSeconds(),
                config.getQueueCapacity(),
                config.getThreadNamePrefix(),
                config.getRejectionPolicy()
        );

        return executor;
    }

    /**
     * Convert RejectionPolicy enum to RejectedExecutionHandler
     */
    private RejectedExecutionHandler getRejectedExecutionHandler(ExecutorConfig.RejectionPolicy policy) {
        switch (policy) {
            case CALLER_RUNS:
                return new ThreadPoolExecutor.CallerRunsPolicy();
            case ABORT:
                return new ThreadPoolExecutor.AbortPolicy();
            case DISCARD:
                return new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST:
                return new ThreadPoolExecutor.DiscardOldestPolicy();
            default:
                return new ThreadPoolExecutor.CallerRunsPolicy();
        }
    }

    /**
     * Get thread pool statistics
     *
     * @param poolType Pool type
     * @return Statistics string
     */
    public String getStatistics(PoolType poolType) {
        ThreadPoolExecutor executor = executorMap.get(poolType);
        if (executor == null) {
            return poolType + ": Not initialized";
        }

        return String.format(
                "%s[active=%d, poolSize=%d, corePoolSize=%d, maxPoolSize=%d, queueSize=%d, completedTasks=%d]",
                poolType,
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount()
        );
    }

    /**
     * Shutdown specific thread pool
     *
     * @param poolType Pool type
     */
    public void shutdown(PoolType poolType) {
        ThreadPoolExecutor executor = executorMap.remove(poolType);
        if (executor != null) {
            shutdownExecutor(poolType.name(), executor);
        }
        if (poolType == PoolType.ISOLATED_EXECUTOR) {
            sharedIsolatedExecutor = null;
        }
    }

    /**
     * Shutdown all thread pools
     */
    public void shutdownAll() {
        logger.info("Shutting down all thread pools...");
        executorMap.forEach((poolType, executor) -> shutdownExecutor(poolType.name(), executor));
        executorMap.clear();
        configMap.clear();
        threadCounterMap.clear();
        sharedIsolatedExecutor = null;
        logger.info("All thread pools shutdown complete");
    }

    /**
     * Shutdown executor gracefully
     */
    private void shutdownExecutor(String name, ThreadPoolExecutor executor) {
        logger.info("Shutting down thread pool: {}", name);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    logger.error("Thread pool {} did not terminate", name);
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("Thread pool {} shutdown complete", name);
    }
}
