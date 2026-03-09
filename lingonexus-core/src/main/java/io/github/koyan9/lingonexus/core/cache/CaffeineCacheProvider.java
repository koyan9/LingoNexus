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

package io.github.koyan9.lingonexus.core.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.koyan9.lingonexus.api.config.CacheConfig;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存提供者实现
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class CaffeineCacheProvider<K, V> implements CacheProvider<K, V> {

    private final Cache<K, V> cache;

    public CaffeineCacheProvider(CacheConfig config) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(config.getMaxSize())
                .expireAfterWrite(config.getExpireAfterWriteMs(), TimeUnit.MILLISECONDS)
                .expireAfterAccess(config.getExpireAfterAccessMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Override
    public V get(K key) {
        return cache.getIfPresent(key);
    }

    @Override
    public void put(K key, V value) {
        cache.put(key, value);
    }

    @Override
    public V computeIfAbsent(K key, java.util.function.Function<K, V> mappingFunction) {
        return cache.get(key, mappingFunction);
    }

    @Override
    public void remove(K key) {
        cache.invalidate(key);
    }

    @Override
    public void clear() {
        cache.invalidateAll();
    }

    @Override
    public long size() {
        return cache.estimatedSize();
    }
}
