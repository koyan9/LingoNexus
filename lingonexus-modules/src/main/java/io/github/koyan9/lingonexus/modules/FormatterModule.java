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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 格式化模块 - 提供数据格式化功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * formatter.number(1234567.89, "#,##0.00")     // "1,234,567.89"
 * formatter.currency(1234.5)                    // "1,234.50"
 * formatter.percent(0.856)                      // "85.60%"
 * formatter.padLeft("42", 5, '0')               // "00042"
 * formatter.padRight("hi", 5, '.')              // "hi..."
 * formatter.mask("13800138000", 3, 7)           // "138****8000"
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class FormatterModule implements ScriptModule {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_FORMATTER;
    private final Map<String, Object> functions;

    public FormatterModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    @SuppressWarnings("unchecked")
    private void initFunctions() {
        functions.put("number", (BiFunction<Number, String, String>) this::number);
        functions.put("currency", (Function<Number, String>) this::currency);
        functions.put("percent", (Function<Number, String>) this::percent);
        functions.put("padLeft", (TriFunction<String, Integer, Character, String>) this::padLeft);
        functions.put("padRight", (TriFunction<String, Integer, Character, String>) this::padRight);
        functions.put("mask", (TriFunction<String, Integer, Integer, String>) this::mask);
        functions.put("capitalize", (Function<String, String>) this::capitalize);
        functions.put("camelCase", (Function<String, String>) this::camelCase);
        functions.put("snakeCase", (Function<String, String>) this::snakeCase);
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
        return "Formatting utility module for numbers, strings and data masking";
    }

    public String number(Number value, String pattern) {
        if (value == null) {
            return "";
        }
        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(value);
    }

    public String currency(Number value) {
        if (value == null) {
            return "";
        }
        BigDecimal bd = new BigDecimal(value.toString()).setScale(2, RoundingMode.HALF_UP);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        return df.format(bd);
    }

    public String percent(Number value) {
        if (value == null) {
            return "";
        }
        BigDecimal bd = new BigDecimal(value.toString())
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        return bd.toPlainString() + "%";
    }

    public String padLeft(String str, int length, char padChar) {
        if (str == null) str = "";
        if (str.length() >= length) return str;
        StringBuilder sb = new StringBuilder();
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        sb.append(str);
        return sb.toString();
    }

    public String padRight(String str, int length, char padChar) {
        if (str == null) str = "";
        if (str.length() >= length) return str;
        StringBuilder sb = new StringBuilder(str);
        for (int i = str.length(); i < length; i++) {
            sb.append(padChar);
        }
        return sb.toString();
    }

    public String mask(String str, int start, int end) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        int len = str.length();
        int s = Math.max(0, Math.min(start, len));
        int e = Math.max(s, Math.min(end, len));

        StringBuilder sb = new StringBuilder();
        sb.append(str, 0, s);
        for (int i = s; i < e; i++) {
            sb.append('*');
        }
        sb.append(str, e, len);
        return sb.toString();
    }

    public String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public String camelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] parts = str.split("[_\\-\\s]+");
        StringBuilder sb = new StringBuilder(parts[0].toLowerCase());
        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                sb.append(Character.toUpperCase(parts[i].charAt(0)));
                sb.append(parts[i].substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    public String snakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else if (c == '-' || c == ' ') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
