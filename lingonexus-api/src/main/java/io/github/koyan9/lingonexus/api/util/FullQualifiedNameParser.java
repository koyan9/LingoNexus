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

package io.github.koyan9.lingonexus.api.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用黑名单解析工具（支持 java.io.* 和 java.io.** 两种通配符）
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-06
 */
public class FullQualifiedNameParser {

    private static final Logger logger = LoggerFactory.getLogger(FullQualifiedNameParser.class);

    // 正则：匹配递归包通配符（**），如 java.io.** → 分组1为 java.io
    private static final Pattern RECURSIVE_PACKAGE_PATTERN = Pattern.compile("^(\\S+)\\.\\*\\*$");
    // 正则：匹配单层包通配符（*），如 java.io.* → 分组1为 java.io
    private static final Pattern SINGLE_PACKAGE_PATTERN = Pattern.compile("^(\\S+)\\.\\*$");

    /**
     * 解析黑名单（支持精确类名、单层包*、递归包**）
     * @param rawPackageList 原始全限定名（如 ["java.io.*", "java.net.**", "java.lang.Runtime"]）
     * @return 解析结果：
     *         <p>- singlePackages: 单层包（*），如 [java.io]</p>
     *         <p>- recursivePackages: 递归包（**），如 [java.net]</p>
     *         <p>- exactClasses: 精确类名，如 [java.lang.Runtime]</p>
     */
    public static PackageListParseResult parseFullQualifiedNameList(Set<String> rawPackageList) {
        Set<String> singlePackages = new HashSet<>();   // 单层包（*）
        Set<String> recursivePackages = new HashSet<>();// 递归包（**）
        Set<String> exactClasses = new HashSet<>();     // 精确类名

        for (String item : rawPackageList) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            String cleanItem = item.trim();

            // 第一步：匹配递归包通配符（**）
            var recursiveMatcher = RECURSIVE_PACKAGE_PATTERN.matcher(cleanItem);
            if (recursiveMatcher.matches()) {
                String pkg = recursiveMatcher.group(1);
                recursivePackages.add(pkg);
                logger.debug("Analyze and extract recursion-forbidden packages: {}", pkg);
                continue;
            }

            // 第二步：匹配单层包通配符（*）
            var singleMatcher = SINGLE_PACKAGE_PATTERN.matcher(cleanItem);
            if (singleMatcher.matches()) {
                String pkg = singleMatcher.group(1);
                singlePackages.add(pkg);
                logger.debug("Analyze the single-layer prohibited package: {}", pkg);
                continue;
            }

            // 第三步：无通配符 → 精确类名
            exactClasses.add(cleanItem);
            logger.debug("Analyze and extract precise prohibited categories: {}", cleanItem);
        }

        return new PackageListParseResult(singlePackages, recursivePackages, exactClasses);
    }

    /**
     * 将精确类名字符串转为Class对象（适配Groovy AST）
     */
    public static List<Class<?>> convertToClasses(Set<String> classNames) {
        List<Class<?>> classes = new ArrayList<>();
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                classes.add(clazz);
            } catch (ClassNotFoundException e) {
                logger.warn("Failed to load the prohibited class {}, skipping.", className, e);
            }
        }
        return classes;
    }

    /**
     * 解析结果封装（替代原Map，类型更安全）
     */
    public static class PackageListParseResult {
        private final Set<String> singlePackages;    // 单层包（*）
        private final Set<String> recursivePackages; // 递归包（**）
        private final Set<String> exactClasses;      // 精确类名

        public PackageListParseResult(Set<String> singlePackages, Set<String> recursivePackages, Set<String> exactClasses) {
            this.singlePackages = Collections.unmodifiableSet(singlePackages);
            this.recursivePackages = Collections.unmodifiableSet(recursivePackages);
            this.exactClasses = Collections.unmodifiableSet(exactClasses);
        }

        // Getter
        public Set<String> getSinglePackages() {
            return singlePackages;
        }

        public Set<String> getRecursivePackages() {
            return recursivePackages;
        }

        public Set<String> getExactClasses() {
            return exactClasses;
        }
    }
}
