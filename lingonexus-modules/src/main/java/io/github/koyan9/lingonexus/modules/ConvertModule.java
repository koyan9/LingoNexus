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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 类型转换工具模块 - 提供常用类型转换功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * convert.toString(123)          // "123"
 * convert.toInteger("42")        // 42
 * convert.toLong("1000000")      // 1000000L
 * convert.toDouble("3.14")       // 3.14
 * convert.toBoolean("true")      // true
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class ConvertModule implements ScriptModule, UtilityFunctions.Convert {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_CONVERT;
    private final Map<String, Object> functions;

    public ConvertModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("toString", (Function<Object, String>) this::toString);
        functions.put("toInteger", (Function<Object, Integer>) this::toInteger);
        functions.put("toLong", (Function<Object, Long>) this::toLong);
        functions.put("toDouble", (Function<Object, Double>) this::toDouble);
        functions.put("toBoolean", (Function<Object, Boolean>) this::toBoolean);
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
        return "Type conversion utility module";
    }

    @Override
    public String toString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    @Override
    public Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(String.valueOf(value).trim());
    }

    @Override
    public Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(String.valueOf(value).trim());
    }

    @Override
    public Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Double) {
            return (Double) value;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return Double.parseDouble(String.valueOf(value).trim());
    }

    @Override
    public Boolean toBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String str = String.valueOf(value).trim().toLowerCase();
        return "true".equals(str) || "1".equals(str) || "yes".equals(str);
    }
}
