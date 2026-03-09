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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局无状态对象缓存
 *
 * <p>用于缓存实现了 {@link io.github.koyan9.lingonexus.api.marker.Stateless} 接口的对象实例。
 * 这些对象是无状态的，可以安全地在多个引擎实例之间共享。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>缓存无状态的 ScriptModule 实例（如 MathModule、StringModule）</li>
 *   <li>缓存无状态的 Sandbox 实例（如 JavaSandbox）</li>
 *   <li>缓存其他无状态的工具类实例</li>
 * </ul>
 *
 * <p>线程安全：使用 ConcurrentHashMap 保证线程安全</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-04
 */
public class GlobalStatelessCache {

    private static final Logger logger = LoggerFactory.getLogger(GlobalStatelessCache.class);

    /**
     * 全局缓存存储
     * Key: 类全限定名，Value: 对象实例
     */
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();

    /**
     * 私有构造函数，防止实例化
     */
    private GlobalStatelessCache() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 获取或创建缓存对象
     *
     * <p>如果缓存中存在，直接返回；否则使用提供的工厂方法创建并缓存。</p>
     *
     * @param key 缓存键（通常是类全限定名）
     * @param factory 对象创建工厂
     * @param <T> 对象类型
     * @return 缓存的对象实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T getOrCreate(String key, ObjectFactory<T> factory) {
        return (T) CACHE.computeIfAbsent(key, k -> {
            T instance = factory.create();
            logger.info("Created and cached stateless object: {}", key);
            return instance;
        });
    }

    /**
     * 直接获取缓存对象
     *
     * @param key 缓存键
     * @param <T> 对象类型
     * @return 缓存的对象，如果不存在返回 null
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) CACHE.get(key);
    }

    /**
     * 直接放入缓存
     *
     * @param key 缓存键
     * @param value 对象实例
     */
    public static void put(String key, Object value) {
        CACHE.put(key, value);
        logger.debug("Cached stateless object: {}", key);
    }

    /**
     * 移除指定缓存
     *
     * @param key 缓存键
     * @return 被移除的对象，如果不存在返回 null
     */
    public static Object remove(String key) {
        Object removed = CACHE.remove(key);
        if (removed != null) {
            logger.info("Removed cached object: {}", key);
        }
        return removed;
    }

    /**
     * 清空所有缓存
     */
    public static void clear() {
        int size = CACHE.size();
        CACHE.clear();
        logger.info("Cleared all stateless cache, removed {} objects", size);
    }

    /**
     * 获取缓存大小
     *
     * @return 缓存中的对象数量
     */
    public static int size() {
        return CACHE.size();
    }

    /**
     * 检查是否包含指定键
     *
     * @param key 缓存键
     * @return 如果存在返回 true
     */
    public static boolean contains(String key) {
        return CACHE.containsKey(key);
    }

    /**
     * 对象创建工厂接口
     *
     * @param <T> 对象类型
     */
    @FunctionalInterface
    public interface ObjectFactory<T> {
        /**
         * 创建对象实例
         *
         * @return 新创建的对象
         */
        T create();
    }
}
