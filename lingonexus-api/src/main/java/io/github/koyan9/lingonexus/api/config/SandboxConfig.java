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

import io.github.koyan9.lingonexus.api.executor.isolation.ExecutionIsolationMode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 沙箱配置 - 控制脚本执行的安全限制和性能优化
 *
 * <p>SandboxConfig 定义了脚本执行的安全边界和性能优化选项：</p>
 *
 * <h3>安全限制配置：</h3>
 * <ul>
 *   <li><b>enabled</b>: 是否启用沙箱安全检查（默认：true）</li>
 *   <li><b>maxScriptSize</b>: 最大脚本大小，单位字节（默认：65536 = 64KB）</li>
 *   <li><b>timeoutMs</b>: 脚本执行超时时间，单位毫秒（默认：5000ms = 5秒）</li>
 *   <li><b>classWhitelist</b>: 类白名单，允许访问的类列表（支持通配符 *）</li>
 *   <li><b>classBlacklist</b>: 类黑名单，禁止访问的类列表（支持通配符 *，优先级高于白名单）</li>
 * </ul>
 *
 * <h3>性能优化配置：</h3>
 * <ul>
 *   <li><b>enableEngineCache</b>: 是否启用引擎内部缓存（默认：true）
 *     <ul>
 *       <li>仅对支持内部缓存的引擎有效（如 JavaExprSandbox 使用的 liquor-eval）</li>
 *       <li>对于不支持内部缓存的引擎（Groovy, GraalJS, Kotlin, Janino），此配置无效</li>
 *       <li>这些引擎依赖 ScriptCacheManager 的 Caffeine 缓存（由 CacheConfig 控制）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>缓存层次说明：</h3>
 * <table border="1">
 *   <tr>
 *     <th>配置项</th>
 *     <th>作用范围</th>
 *     <th>控制对象</th>
 *     <th>适用引擎</th>
 *   </tr>
 *   <tr>
 *     <td>CacheConfig.enabled</td>
 *     <td>ScriptExecutor 层</td>
 *     <td>Caffeine 缓存（编译缓存 + 执行缓存）</td>
 *     <td>所有脚本引擎</td>
 *   </tr>
 *   <tr>
 *     <td>SandboxConfig.enableEngineCache</td>
 *     <td>Sandbox 层</td>
 *     <td>引擎内部缓存</td>
 *     <td>仅 JavaExprSandbox (liquor-eval)</td>
 *   </tr>
 * </table>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 * @see CacheConfig
 */
public class SandboxConfig {

    private final boolean enabled;
    private final int maxScriptSize;
    private final long timeoutMs;
    private final boolean enableEngineCache;
    private final ExecutionIsolationMode isolationMode;
    private final int externalProcessPoolSize;
    private final int externalProcessStartupRetries;
    private final int externalProcessPrewarmCount;
    private final long externalProcessIdleTtlMs;
    private final int externalProcessExecutorCacheMaxSize;
    private final long externalProcessExecutorCacheIdleTtlMs;
    private final Set<String> classWhitelist;
    private final Set<String> classBlacklist;

    private SandboxConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.maxScriptSize = builder.maxScriptSize;
        this.timeoutMs = builder.timeoutMs;
        this.enableEngineCache = builder.enableEngineCache;
        this.isolationMode = builder.isolationMode;
        this.externalProcessPoolSize = builder.externalProcessPoolSize;
        this.externalProcessStartupRetries = builder.externalProcessStartupRetries;
        this.externalProcessPrewarmCount = builder.externalProcessPrewarmCount;
        this.externalProcessIdleTtlMs = builder.externalProcessIdleTtlMs;
        this.externalProcessExecutorCacheMaxSize = builder.externalProcessExecutorCacheMaxSize;
        this.externalProcessExecutorCacheIdleTtlMs = builder.externalProcessExecutorCacheIdleTtlMs;
        this.classWhitelist = Collections.unmodifiableSet(new HashSet<>(builder.classWhitelist));
        this.classBlacklist = Collections.unmodifiableSet(new HashSet<>(builder.classBlacklist));
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxScriptSize() {
        return maxScriptSize;
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public boolean isEnableEngineCache() {
        return enableEngineCache;
    }

    public ExecutionIsolationMode getIsolationMode() {
        return isolationMode;
    }

    public int getExternalProcessPoolSize() {
        return externalProcessPoolSize;
    }

    public int getExternalProcessStartupRetries() {
        return externalProcessStartupRetries;
    }

    public int getExternalProcessPrewarmCount() {
        return externalProcessPrewarmCount;
    }

    public long getExternalProcessIdleTtlMs() {
        return externalProcessIdleTtlMs;
    }

    public int getExternalProcessExecutorCacheMaxSize() {
        return externalProcessExecutorCacheMaxSize;
    }

    public long getExternalProcessExecutorCacheIdleTtlMs() {
        return externalProcessExecutorCacheIdleTtlMs;
    }

    /**
     * 获取类白名单
     *
     * @return 不可变的类白名单集合
     */
    public Set<String> getClassWhitelist() {
        return classWhitelist;
    }

    /**
     * 获取类黑名单
     *
     * @return 不可变的类黑名单集合
     */
    public Set<String> getClassBlacklist() {
        return classBlacklist;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private int maxScriptSize = 65536;
        private long timeoutMs = 5000;
        private boolean enableEngineCache = true;
        private ExecutionIsolationMode isolationMode = ExecutionIsolationMode.AUTO;
        private int externalProcessPoolSize = 1;
        private int externalProcessStartupRetries = 2;
        private int externalProcessPrewarmCount = 1;
        private long externalProcessIdleTtlMs = 300000L;
        private int externalProcessExecutorCacheMaxSize = 8;
        private long externalProcessExecutorCacheIdleTtlMs = 300000L;
        private Set<String> classWhitelist = createDefaultWhitelist();
        private Set<String> classBlacklist = createDefaultBlacklist();

        /**
         * 创建默认白名单
         * <p>包含常用的安全 Java 类</p>
         */
        private static Set<String> createDefaultWhitelist() {
            return new HashSet<>();
        }

        /**
         * 创建默认黑名单
         * <p>包含危险的 Java 类</p>
         */
        private static Set<String> createDefaultBlacklist() {
            return new HashSet<>();
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder maxScriptSize(int maxScriptSize) {
            this.maxScriptSize = maxScriptSize;
            return this;
        }

        public Builder timeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder enableEngineCache(boolean enableEngineCache) {
            this.enableEngineCache = enableEngineCache;
            return this;
        }

        public Builder isolationMode(ExecutionIsolationMode isolationMode) {
            if (isolationMode != null) {
                this.isolationMode = isolationMode;
            }
            return this;
        }

        public Builder externalProcessPoolSize(int externalProcessPoolSize) {
            if (externalProcessPoolSize <= 0) {
                throw new IllegalArgumentException("externalProcessPoolSize must be positive");
            }
            this.externalProcessPoolSize = externalProcessPoolSize;
            return this;
        }

        public Builder externalProcessStartupRetries(int externalProcessStartupRetries) {
            if (externalProcessStartupRetries < 0) {
                throw new IllegalArgumentException("externalProcessStartupRetries must be non-negative");
            }
            this.externalProcessStartupRetries = externalProcessStartupRetries;
            return this;
        }

        public Builder externalProcessPrewarmCount(int externalProcessPrewarmCount) {
            if (externalProcessPrewarmCount < 0) {
                throw new IllegalArgumentException("externalProcessPrewarmCount must be non-negative");
            }
            this.externalProcessPrewarmCount = externalProcessPrewarmCount;
            return this;
        }

        public Builder externalProcessIdleTtlMs(long externalProcessIdleTtlMs) {
            if (externalProcessIdleTtlMs < 0) {
                throw new IllegalArgumentException("externalProcessIdleTtlMs must be non-negative");
            }
            this.externalProcessIdleTtlMs = externalProcessIdleTtlMs;
            return this;
        }

        public Builder externalProcessExecutorCacheMaxSize(int externalProcessExecutorCacheMaxSize) {
            if (externalProcessExecutorCacheMaxSize <= 0) {
                throw new IllegalArgumentException("externalProcessExecutorCacheMaxSize must be positive");
            }
            this.externalProcessExecutorCacheMaxSize = externalProcessExecutorCacheMaxSize;
            return this;
        }

        public Builder externalProcessExecutorCacheIdleTtlMs(long externalProcessExecutorCacheIdleTtlMs) {
            if (externalProcessExecutorCacheIdleTtlMs < 0) {
                throw new IllegalArgumentException("externalProcessExecutorCacheIdleTtlMs must be non-negative");
            }
            this.externalProcessExecutorCacheIdleTtlMs = externalProcessExecutorCacheIdleTtlMs;
            return this;
        }

        /**
         * 设置类白名单
         *
         * @param classWhitelist 类白名单集合
         * @return Builder
         */
        public Builder classWhitelist(Set<String> classWhitelist) {
            if (classWhitelist != null) {
                this.classWhitelist = new HashSet<>(classWhitelist);
            }
            return this;
        }

        /**
         * 添加类到白名单
         *
         * @param classPattern 类模式（支持通配符 *）
         * @return Builder
         */
        public Builder addToWhitelist(String classPattern) {
            if (this.classWhitelist != null) {
                this.classWhitelist.add(classPattern);
            }
            return this;
        }

        /**
         * 设置类黑名单
         *
         * @param classBlacklist 类黑名单集合
         * @return Builder
         */
        public Builder classBlacklist(Set<String> classBlacklist) {
            if (classBlacklist != null) {
                this.classBlacklist = new HashSet<>(classBlacklist);
            }
            return this;
        }

        /**
         * 添加类到黑名单
         *
         * @param classPattern 类模式（支持通配符 *）
         * @return Builder
         */
        public Builder addToBlacklist(String classPattern) {
            if (this.classBlacklist != null) {
                this.classBlacklist.add(classPattern);
            }
            return this;
        }

        public SandboxConfig build() {
            return new SandboxConfig(this);
        }
    }
}
