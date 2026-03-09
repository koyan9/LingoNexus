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
package io.github.koyan9.lingonexus.api.config;

/**
 * 异步执行器线程池配置
 *
 * <p>用于配置 DefaultScriptExecutor 中异步执行脚本的线程池参数</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-25
 */
public class ExecutorConfig {

    /**
     * 拒绝策略枚举
     */
    public enum RejectionPolicy {
        /**
         * 调用者运行策略：由调用线程执行任务
         */
        CALLER_RUNS,

        /**
         * 中止策略：抛出 RejectedExecutionException
         */
        ABORT,

        /**
         * 丢弃策略：静默丢弃任务
         */
        DISCARD,

        /**
         * 丢弃最旧策略：丢弃队列中最旧的任务
         */
        DISCARD_OLDEST
    }

    /**
     * 核心线程数
     * 默认值：CPU核心数 * 2
     */
    private final int corePoolSize;

    /**
     * 最大线程数
     * 默认值：max(corePoolSize * 2, 50)
     */
    private final int maxPoolSize;

    /**
     * 线程空闲存活时间（秒）
     * 默认值：60秒
     */
    private final long keepAliveTimeSeconds;

    /**
     * 任务队列容量
     * 默认值：1000
     */
    private final int queueCapacity;

    /**
     * 线程名称前缀
     * 默认值：ScriptExecutor-Async-
     */
    private final String threadNamePrefix;

    /**
     * 拒绝策略
     * 默认值：CALLER_RUNS
     */
    private final RejectionPolicy rejectionPolicy;

    private ExecutorConfig(Builder builder) {
        this.corePoolSize = builder.corePoolSize;
        this.maxPoolSize = builder.maxPoolSize;
        this.keepAliveTimeSeconds = builder.keepAliveTimeSeconds;
        this.queueCapacity = builder.queueCapacity;
        this.threadNamePrefix = builder.threadNamePrefix;
        this.rejectionPolicy = builder.rejectionPolicy;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public long getKeepAliveTimeSeconds() {
        return keepAliveTimeSeconds;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public RejectionPolicy getRejectionPolicy() {
        return rejectionPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        private int maxPoolSize = Math.max(Runtime.getRuntime().availableProcessors() * 4, 50);
        private long keepAliveTimeSeconds = 60L;
        private int queueCapacity = 1000;
        private String threadNamePrefix = "ScriptExecutor-Async-";
        private RejectionPolicy rejectionPolicy = RejectionPolicy.CALLER_RUNS;

        public Builder corePoolSize(int corePoolSize) {
            if (corePoolSize <= 0) {
                throw new IllegalArgumentException("corePoolSize must be positive");
            }
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            if (maxPoolSize <= 0) {
                throw new IllegalArgumentException("maxPoolSize must be positive");
            }
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder keepAliveTimeSeconds(long keepAliveTimeSeconds) {
            if (keepAliveTimeSeconds < 0) {
                throw new IllegalArgumentException("keepAliveTimeSeconds must be non-negative");
            }
            this.keepAliveTimeSeconds = keepAliveTimeSeconds;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            if (queueCapacity <= 0) {
                throw new IllegalArgumentException("queueCapacity must be positive");
            }
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder threadNamePrefix(String threadNamePrefix) {
            if (threadNamePrefix == null || threadNamePrefix.trim().isEmpty()) {
                throw new IllegalArgumentException("threadNamePrefix cannot be null or empty");
            }
            this.threadNamePrefix = threadNamePrefix;
            return this;
        }

        public Builder rejectionPolicy(RejectionPolicy rejectionPolicy) {
            if (rejectionPolicy == null) {
                throw new IllegalArgumentException("rejectionPolicy cannot be null");
            }
            this.rejectionPolicy = rejectionPolicy;
            return this;
        }

        public ExecutorConfig build() {
            // Validate maxPoolSize >= corePoolSize
            if (maxPoolSize < corePoolSize) {
                throw new IllegalArgumentException("maxPoolSize must be >= corePoolSize");
            }
            return new ExecutorConfig(this);
        }
    }
}
