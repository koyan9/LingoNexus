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
 * 缓存配置 - 控制 ScriptCacheManager 的 Caffeine 缓存行为
 *
 * <p>CacheConfig 控制 ScriptExecutor 层的缓存策略，包括：</p>
 * <ul>
 *   <li><b>编译缓存</b>: 缓存 CompiledScript 对象，避免重复编译相同脚本</li>
 *   <li><b>执行缓存</b>: 缓存脚本执行结果，适用于纯函数脚本（相同输入产生相同输出）</li>
 * </ul>
 *
 * <p>配置项说明：</p>
 * <ul>
 *   <li><b>enabled</b>: 是否启用 ScriptCacheManager 缓存（默认：true）</li>
 *   <li><b>maxSize</b>: 缓存最大条目数（默认：1000）</li>
 *   <li><b>expireAfterWriteMs</b>: 写入后过期时间，单位毫秒（默认：3600000ms = 1小时）</li>
 *   <li><b>expireAfterAccessMs</b>: 访问后过期时间，单位毫秒（默认：1800000ms = 30分钟）</li>
 * </ul>
 *
 * <p>注意：此配置仅控制 ScriptExecutor 层的 Caffeine 缓存，不影响引擎内部缓存（如 liquor-eval 的内置缓存）。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 * @see io.github.koyan9.lingonexus.core.cache.ScriptCacheManager
 */
public class CacheConfig {

    private final boolean enabled;
    private final long maxSize;
    private final long expireAfterWriteMs;
    private final long expireAfterAccessMs;

    private CacheConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.maxSize = builder.maxSize;
        this.expireAfterWriteMs = builder.expireAfterWriteMs;
        this.expireAfterAccessMs = builder.expireAfterAccessMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public long getExpireAfterWriteMs() {
        return expireAfterWriteMs;
    }

    public long getExpireAfterAccessMs() {
        return expireAfterAccessMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean enabled = true;
        private long maxSize = 1000;
        private long expireAfterWriteMs = 3600000;
        private long expireAfterAccessMs = 1800000;

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder maxSize(long maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder expireAfterWriteMs(long expireAfterWriteMs) {
            this.expireAfterWriteMs = expireAfterWriteMs;
            return this;
        }

        public Builder expireAfterAccessMs(long expireAfterAccessMs) {
            this.expireAfterAccessMs = expireAfterAccessMs;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(this);
        }
    }
}
