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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 验证器模块 - 提供常用数据验证功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * validator.isEmail("test@example.com")     // true
 * validator.isPhone("13800138000")          // true
 * validator.isUrl("https://example.com")    // true
 * validator.isIdCard("110101199003076530")  // true
 * validator.range(5, 1, 10)                // true
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class ValidatorModule implements ScriptModule {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_VALIDATOR;
    private final Map<String, Object> functions;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern URL_PATTERN =
            Pattern.compile("^https?://[\\w.-]+(?:\\.[\\w.-]+)+[\\w.,@?^=%&:/~+#-]*$");
    private static final Pattern IP_V4_PATTERN =
            Pattern.compile("^(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("^\\d{17}[\\dXx]$");

    public ValidatorModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("isEmail", (Function<String, Boolean>) this::isEmail);
        functions.put("isPhone", (Function<String, Boolean>) this::isPhone);
        functions.put("isUrl", (Function<String, Boolean>) this::isUrl);
        functions.put("isIpv4", (Function<String, Boolean>) this::isIpv4);
        functions.put("isIdCard", (Function<String, Boolean>) this::isIdCard);
        functions.put("isNumber", (Function<String, Boolean>) this::isNumber);
        functions.put("range", (TriFunction<Number, Number, Number, Boolean>) this::range);
        functions.put("lengthRange", (TriFunction<String, Integer, Integer, Boolean>) this::lengthRange);
        functions.put("notNull", (Function<Object, Boolean>) this::notNull);
        functions.put("matches", (BiFunction<String, String, Boolean>) this::matches);
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
        return "Validation utility module for common data validation";
    }

    public boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }

    public boolean isPhone(String value) {
        return value != null && PHONE_PATTERN.matcher(value).matches();
    }

    public boolean isUrl(String value) {
        return value != null && URL_PATTERN.matcher(value).matches();
    }

    public boolean isIpv4(String value) {
        return value != null && IP_V4_PATTERN.matcher(value).matches();
    }

    public boolean isIdCard(String value) {
        return value != null && ID_CARD_PATTERN.matcher(value).matches();
    }

    public boolean isNumber(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(value.trim());
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean range(Number value, Number min, Number max) {
        if (value == null || min == null || max == null) {
            return false;
        }
        double v = value.doubleValue();
        return v >= min.doubleValue() && v <= max.doubleValue();
    }

    public boolean lengthRange(String value, int minLen, int maxLen) {
        if (value == null) {
            return false;
        }
        int len = value.length();
        return len >= minLen && len <= maxLen;
    }

    public boolean notNull(Object value) {
        return value != null;
    }

    public boolean matches(String value, String regex) {
        if (value == null || regex == null) {
            return false;
        }
        return Pattern.matches(regex, value);
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
