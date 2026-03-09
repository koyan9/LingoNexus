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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;
import io.github.koyan9.lingonexus.api.util.UtilityFunctions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * JSON 工具模块 - 提供 JSON 序列化、反序列化和路径解析功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * // 将对象转为 JSON 字符串
 * json.toJson(obj)
 *
 * // 将 JSON 字符串转为对象
 * json.fromJson('{"name":"test"}')
 *
 * // 解析 JSON 路径
 * json.parsePath('{"user":{"name":"test"}}', 'user.name')
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class JsonModule implements ScriptModule, UtilityFunctions.Json {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_JSON;
    private final ObjectMapper objectMapper;
    private final Map<String, Object> functions;

    public JsonModule() {
        this.objectMapper = new ObjectMapper();
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("toJson", (Function<Object, String>) this::toJson);
        functions.put("stringify", (Function<Object, String>) this::stringify);
        functions.put("fromJson", (Function<String, Object>) this::fromJson);
        functions.put("parse", (Function<String, Object>) this::parse);
        functions.put("parsePath", (BiFunction<String, String, Object>) this::parsePath);
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
        return "JSON utility module for serialization, deserialization and path parsing";
    }

    @Override
    public String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }

    @Override
    public String stringify(Object obj) {
        return toJson(obj);
    }

    @Override
    public Object fromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse JSON string", e);
        }
    }

    @Override
    public Object parse(String json) {
        return fromJson(json);
    }

    @Override
    public Object parsePath(String json, String path) {
        if (json == null || path == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            String[] parts = path.split("\\.");
            JsonNode current = root;

            for (String part : parts) {
                if (current == null || current.isMissingNode()) {
                    return null;
                }
                // Handle array index like "items[0]"
                if (part.contains("[")) {
                    int bracketIndex = part.indexOf('[');
                    String fieldName = part.substring(0, bracketIndex);
                    int arrayIndex = Integer.parseInt(part.substring(bracketIndex + 1, part.length() - 1));

                    current = current.get(fieldName);
                    if (current != null && current.isArray()) {
                        current = current.get(arrayIndex);
                    } else {
                        return null;
                    }
                } else {
                    current = current.get(part);
                }
            }

            if (current == null || current.isMissingNode()) {
                return null;
            }

            // Convert JsonNode to appropriate Java type
            if (current.isTextual()) {
                return current.asText();
            } else if (current.isNumber()) {
                if (current.isInt()) {
                    return current.asInt();
                } else if (current.isLong()) {
                    return current.asLong();
                } else {
                    return current.asDouble();
                }
            } else if (current.isBoolean()) {
                return current.asBoolean();
            } else if (current.isNull()) {
                return null;
            } else {
                return objectMapper.treeToValue(current, Object.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON path: " + path, e);
        }
    }
}
