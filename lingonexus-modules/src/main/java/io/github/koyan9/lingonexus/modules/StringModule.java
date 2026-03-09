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

package io.github.koyan9.lingonexus.modules;

import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.util.UtilityFunctions;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 字符串工具模块 - 提供字符串处理功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * str.isBlank("  ")       // true
 * str.trim("  hello  ")   // "hello"
 * str.split("a,b,c", ",") // ["a", "b", "c"]
 * str.join(["a","b"], "-") // "a-b"
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class StringModule implements ScriptModule, UtilityFunctions.StringUtils {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_STRING;
    private final Map<String, Object> functions;

    public StringModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("reverse", (Function<String, String>) this::reverse);
        functions.put("isBlank", (Function<String, Boolean>) this::isBlank);
        functions.put("isNotBlank", (Function<String, Boolean>) this::isNotBlank);
        functions.put("trim", (Function<String, String>) this::trim);
        functions.put("length", (Function<String, Integer>) this::length);
        functions.put("substring", (TriFunction<String, Integer, Integer, String>) this::substring);
        functions.put("matches", (BiFunction<String, String, Boolean>) this::matches);
        functions.put("split", (BiFunction<String, String, List<String>>) this::split);
        functions.put("join", (BiFunction<Collection<String>, String, String>) this::join);
        functions.put("contains", (BiFunction<String, String, Boolean>) this::contains);
        functions.put("startsWith", (BiFunction<String, String, Boolean>) this::startsWith);
        functions.put("endsWith", (BiFunction<String, String, Boolean>) this::endsWith);
        functions.put("replace", (TriFunction<String, String, String, String>) this::replace);
        functions.put("toUpperCase", (Function<String, String>) this::toUpperCase);
        functions.put("toLowerCase", (Function<String, String>) this::toLowerCase);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public Map<String, Object> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    @Override
    public boolean hasFunction(String functionName) {
        return functions.containsKey(functionName);
    }

    @Override
    public String getDescription() {
        return "String utility module for text processing";
    }

    @Override
    public String reverse(String str) {
        // 处理null值，保持你原代码的逻辑
        if (str == null) {
            return null;
        }
        // 处理空字符串/单字符字符串（避免不必要的对象创建，提升性能）
        int length = str.length();
        if (length <= 1) {
            return str;
        }
        // 核心：使用StringBuilder实现高性能翻转
        return new StringBuilder(str).reverse().toString();
    }

    @Override
    public boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    @Override
    public boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    @Override
    public String trim(String str) {
        return str == null ? null : str.trim();
    }

    @Override
    public int length(String str) {
        return str == null ? 0 : str.length();
    }

    @Override
    public String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (start < 0) start = 0;
        if (end > len) end = len;
        if (start > end) return "";
        return str.substring(start, end);
    }

    @Override
    public boolean matches(String str, String regex) {
        if (str == null || regex == null) {
            return false;
        }
        return Pattern.matches(regex, str);
    }

    @Override
    public List<String> split(String str, String delimiter) {
        if (str == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(str.split(Pattern.quote(delimiter)));
    }

    @Override
    public String join(Collection<String> strings, String delimiter) {
        if (strings == null || strings.isEmpty()) {
            return "";
        }
        return String.join(delimiter, strings);
    }

    public boolean contains(String str, String searchStr) {
        return str != null && searchStr != null && str.contains(searchStr);
    }

    public boolean startsWith(String str, String prefix) {
        return str != null && prefix != null && str.startsWith(prefix);
    }

    public boolean endsWith(String str, String suffix) {
        return str != null && suffix != null && str.endsWith(suffix);
    }

    public String replace(String str, String target, String replacement) {
        if (str == null) return null;
        return str.replace(target, replacement);
    }

    public String toUpperCase(String str) {
        return str == null ? null : str.toUpperCase();
    }

    public String toLowerCase(String str) {
        return str == null ? null : str.toLowerCase();
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
