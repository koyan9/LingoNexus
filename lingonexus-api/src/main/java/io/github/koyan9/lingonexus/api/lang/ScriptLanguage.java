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

package io.github.koyan9.lingonexus.api.lang;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 脚本语言枚举 - 定义 LingoNexus 支持的所有脚本语言
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public enum ScriptLanguage {

    /**
     * Groovy 脚本语言
     */
    GROOVY("groovy", "Groovy", Collections.singletonList("groovy")),

    /**
     * JavaScript 脚本语言 (GraalJS)
     */
    JAVASCRIPT("javascript", "JavaScript", Arrays.asList("javascript", "js", "ecmascript")),

    /**
     * Java 表达式引擎 (liquor-eval) - 仅支持表达式求值
     */
    JAVAEXPR("javaexpr", "Java Expression", Arrays.asList("javaexpr", "jexpr")),

    /**
     * Java 脚本引擎 (Janino) - 支持完整 Java 语法
     */
    JAVA("java", "Java Script", Arrays.asList("java", "janino")),

    /**
     * Kotlin 脚本语言
     */
    KOTLIN("kotlin", "Kotlin", Arrays.asList("kotlin", "kts"));

    /**
     * 语言标识符（主要标识）
     */
    private final String id;

    /**
     * 语言显示名称
     */
    private final String displayName;

    /**
     * 语言别名列表（包括主要标识）
     */
    private final List<String> aliases;

    ScriptLanguage(String id, String displayName, List<String> aliases) {
        this.id = id;
        this.displayName = displayName;
        this.aliases = Collections.unmodifiableList(aliases);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getAliases() {
        return aliases;
    }

    /**
     * 检查给定的语言标识是否匹配此语言
     *
     * @param language 语言标识
     * @return 是否匹配
     */
    public boolean matches(String language) {
        if (language == null || language.trim().isEmpty()) {
            return false;
        }
        String lower = language.toLowerCase().trim();
        return aliases.contains(lower);
    }

    /**
     * 根据语言标识查找对应的枚举值
     *
     * @param language 语言标识（支持别名）
     * @return 对应的枚举值，未找到返回 null
     */
    public static ScriptLanguage fromString(String language) {
        if (language == null || language.trim().isEmpty()) {
            return null;
        }
        String lower = language.toLowerCase().trim();
        for (ScriptLanguage lang : values()) {
            if (lang.aliases.contains(lower)) {
                return lang;
            }
        }
        return null;
    }

    /**
     * 根据语言标识查找对应的枚举值，未找到时抛出异常
     *
     * @param language 语言标识
     * @return 对应的枚举值
     * @throws IllegalArgumentException 如果语言标识无效
     */
    public static ScriptLanguage fromStringOrThrow(String language) {
        ScriptLanguage result = fromString(language);
        if (result == null) {
            throw new IllegalArgumentException("Unsupported script language: " + language);
        }
        return result;
    }

    /**
     * 检查给定的语言标识是否被支持
     *
     * @param language 语言标识
     * @return 是否支持
     */
    public static boolean isSupported(String language) {
        return fromString(language) != null;
    }

    /**
     * 获取默认语言
     *
     * @return 默认语言（Groovy）
     */
    public static ScriptLanguage getDefault() {
        return GROOVY;
    }

    @Override
    public String toString() {
        return id;
    }
}
