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

import java.util.function.Function;

/**
 * 缓存提供者接口
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public interface CacheProvider<K, V> {

    /**
     * 获取缓存值
     *
     * @param key 缓存键
     * @return 缓存值
     */
    V get(K key);

    /**
     * 放入缓存
     *
     * @param key 缓存键
     * @param value 缓存值
     */
    void put(K key, V value);

    /**
     * 原子性获取或计算缓存值
     * <p>如果缓存中不存在该键,则使用 mappingFunction 计算值并缓存</p>
     * <p>此方法是线程安全的,可以防止缓存穿透问题</p>
     *
     * @param key 缓存键
     * @param mappingFunction 计算函数
     * @return 缓存值
     */
    V computeIfAbsent(K key, Function<K, V> mappingFunction);

    /**
     * 移除缓存
     *
     * @param key 缓存键
     */
    void remove(K key);

    /**
     * 清空缓存
     */
    void clear();

    /**
     * 获取缓存大小
     *
     * @return 缓存大小
     */
    long size();
}
