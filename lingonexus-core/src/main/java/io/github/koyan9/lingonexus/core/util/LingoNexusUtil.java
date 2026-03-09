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

package io.github.koyan9.lingonexus.core.util;

import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 工具类
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class LingoNexusUtil {

    private static final Logger logger = LoggerFactory.getLogger(LingoNexusUtil.class);

    /**
     * 判断模块是否被允许加载
     * <p>
     * 支持两种配置方式：
     * <ul>
     *   <li>类全限定名：如 "io.github.koyan9.lingonexus.utils.MathModule"（包含 .）</li>
     *   <li>模块名称：如 "math"（不包含 .）</li>
     *   <li>通配符：如 "io.github.koyan9.lingonexus.utils.*"</li>
     * </ul>
     *
     * @param module               待判断的模块实例
     * @param excludeScriptModules 排除集合（黑名单），null/空表示无排除
     * @param allowedScriptModules 允许集合（白名单），null/空表示无白名单，默认放行未被排除的模块
     * @return true=允许，false=禁止
     */
    public static boolean isModuleAllowed(ScriptModule module, Set<String> excludeScriptModules, Set<String> allowedScriptModules) {
        if (module == null) {
            return false;
        }

        String moduleName = module.getName();
        String moduleClassName = module.getClass().getName();

        // 1. 检查黑名单（优先级最高）
        if (excludeScriptModules != null && !excludeScriptModules.isEmpty()) {
            for (String exclude : excludeScriptModules) {
                if (matchesPattern(exclude, moduleName, moduleClassName)) {
                    logger.warn("Module '{}' (class: {}) is excluded by pattern '{}'", moduleName, moduleClassName, exclude);
                    return false;
                }
            }
        }

        // 2. 检查白名单
        if (allowedScriptModules != null && !allowedScriptModules.isEmpty()) {
            for (String allowed : allowedScriptModules) {
                if (matchesPattern(allowed, moduleName, moduleClassName)) {
                    logger.info("Module '{}' (class: {}) is allowed by pattern '{}'", moduleName, moduleClassName, allowed);
                    return true;
                }
            }
            logger.warn("Module '{}' (class: {}) is not in the allowed list", moduleName, moduleClassName);
            return false;
        }

        // 3. 白名单为空 → 默认放行所有未被排除的模块
        logger.info("Module '{}' (class: {}) is allowed by default (no whitelist configured)", moduleName, moduleClassName);
        return true;
    }

    /**
     * 判断模块是否匹配指定的模式
     *
     * @param pattern         配置的模式（可以是类名、模块名或通配符）
     * @param moduleName      模块名称
     * @param moduleClassName 模块类全限定名
     * @return true=匹配，false=不匹配
     */
    private static boolean matchesPattern(String pattern, String moduleName, String moduleClassName) {
        if (pattern == null || pattern.trim().isEmpty()) {
            return false;
        }

        pattern = pattern.trim();

        // 1. 如果模式包含 '.'，视为类名模式
        if (pattern.contains(".")) {
            // 支持通配符匹配
            if (pattern.endsWith("*")) {
                String prefix = pattern.substring(0, pattern.length() - 1);
                return moduleClassName.startsWith(prefix);
            }
            // 精确匹配类名
            return pattern.equals(moduleClassName);
        }

        // 2. 否则视为模块名称模式
        // 支持通配符匹配
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            return moduleName.startsWith(prefix);
        }

        // 精确匹配模块名
        return pattern.equals(moduleName);
    }

}
