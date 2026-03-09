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
package io.github.koyan9.lingonexus.api.context;

import io.github.koyan9.lingonexus.api.config.LingoNexusConfig;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;

/**
 * LingoNexus 全局上下文管理器
 * <p>提供全局静态访问 LingoNexusConfig 和 LingoNexusExecutor 的能力</p>
 *
 * <p>使用示例:</p>
 * <pre>{@code
 * // 1. 通过 LingoNexusBuilder 初始化（会自动设置上下文）
 * LingoNexusConfig config = LingoNexusConfig.builder()
 *     .defaultLanguage(ScriptLanguage.GROOVY)
 *     .build();
 * LingoNexusExecutor executor = LingoNexusBuilder.loadInstance(config);
 *
 * // 2. 在任何地方获取配置
 * LingoNexusConfig currentConfig = LingoNexusContext.getConfig();
 * LingoNexusExecutor currentExecutor = LingoNexusContext.getExecutor();
 * }</pre>
 *
 * <p>注意事项:</p>
 * <ul>
 *   <li>必须先通过 LingoNexusBuilder 初始化才能使用</li>
 *   <li>线程安全，使用 volatile 保证可见性</li>
 *   <li>提供 clear() 方法用于测试或重新初始化</li>
 * </ul>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-03-03
 */
public class LingoNexusContext {

    /**
     * 当前全局配置（使用 volatile 保证线程可见性）
     */
    private static volatile LingoNexusConfig currentConfig;

    /**
     * 当前全局执行器（使用 volatile 保证线程可见性）
     */
    private static volatile LingoNexusExecutor currentExecutor;

    /**
     * 私有构造函数，防止实例化
     */
    private LingoNexusContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 设置全局上下文
     * <p>此方法由 LingoNexusBuilder 自动调用，用户通常不需要手动调用</p>
     *
     * @param config 配置对象
     * @param executor 执行器对象
     * @throws IllegalArgumentException 如果参数为 null
     */
    public static synchronized void setContext(LingoNexusConfig config, LingoNexusExecutor executor) {
        if (config == null) {
            throw new IllegalArgumentException("LingoNexusConfig cannot be null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("LingoNexusExecutor cannot be null");
        }
        currentConfig = config;
        currentExecutor = executor;
    }

    /**
     * 获取全局配置
     *
     * @return 当前配置对象
     * @throws IllegalStateException 如果 LingoNexus 尚未初始化
     */
    public static LingoNexusConfig getConfig() {
        if (currentConfig == null) {
            throw new IllegalStateException("LingoNexus has not been initialized. Please call LingoNexusBuilder.loadInstance() first.");
        }
        return currentConfig;
    }

    /**
     * 获取全局执行器
     *
     * @return 当前执行器对象
     * @throws IllegalStateException 如果 LingoNexus 尚未初始化
     */
    public static LingoNexusExecutor getExecutor() {
        if (currentExecutor == null) {
            throw new IllegalStateException("LingoNexus has not been initialized. Please call LingoNexusBuilder.loadInstance() first.");
        }
        return currentExecutor;
    }

    /**
     * 检查是否已初始化
     *
     * @return 如果已初始化返回 true，否则返回 false
     */
    public static boolean isInitialized() {
        return currentConfig != null && currentExecutor != null;
    }

    /**
     * 清除全局上下文
     * <p>此方法主要用于测试场景或需要重新初始化的场景</p>
     * <p>注意：清除后必须重新调用 LingoNexusBuilder.loadInstance() 才能继续使用</p>
     */
    public static void clear() {
        currentConfig = null;
        currentExecutor = null;
    }
}
