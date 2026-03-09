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
package io.github.koyan9.lingonexus.core.process;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Normalizes values into JSON-safe structures.
 */
public final class JsonSafeValueNormalizer {

    private JsonSafeValueNormalizer() {
    }

    public static Object normalize(Object value) {
        return normalize(value, "$", false);
    }

    public static Object normalizeForRequest(Object value) {
        return normalize(value, "$", false);
    }

    public static Object normalizeForResponse(Object value) {
        return normalize(value, "$", true);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeMap(Map<?, ?> value) {
        return (Map<String, Object>) normalize(value, "$", false);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> normalizeMap(Map<?, ?> value, String path, boolean allowStringFallback) {
        return (Map<String, Object>) normalize(value, path, allowStringFallback);
    }

    private static Object normalize(Object value, String path, boolean allowStringFallback) {
        if (value == null
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean) {
            return value;
        }

        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }

        if (value instanceof Map<?, ?>) {
            return normalizeMapInternal((Map<?, ?>) value, path, allowStringFallback);
        }

        if (value instanceof Collection<?>) {
            return normalizeCollection((Collection<?>) value, path, allowStringFallback);
        }

        if (value.getClass().isArray()) {
            return normalizeArray(value, path, allowStringFallback);
        }

        if (allowStringFallback) {
            return String.valueOf(value);
        }

        throw new IllegalArgumentException(
                "Unsupported value at " + path + ": " + value.getClass().getName()
        );
    }

    private static Map<String, Object> normalizeMapInternal(Map<?, ?> value, String path, boolean allowStringFallback) {
        Map<String, Object> normalized = new LinkedHashMap<String, Object>();
        for (Map.Entry<?, ?> entry : value.entrySet()) {
            Object key = entry.getKey();
            if (!(key instanceof String)) {
                throw new IllegalArgumentException("Unsupported map key at " + path + ": " + key);
            }
            String childPath = path + "." + key;
            normalized.put((String) key, normalize(entry.getValue(), childPath, allowStringFallback));
        }
        return normalized;
    }

    private static List<Object> normalizeCollection(Collection<?> value, String path, boolean allowStringFallback) {
        List<Object> normalized = new ArrayList<Object>(value.size());
        int index = 0;
        for (Object item : value) {
            normalized.add(normalize(item, path + "[" + index + "]", allowStringFallback));
            index++;
        }
        return normalized;
    }

    private static List<Object> normalizeArray(Object array, String path, boolean allowStringFallback) {
        int length = Array.getLength(array);
        List<Object> normalized = new ArrayList<Object>(length);
        for (int index = 0; index < length; index++) {
            normalized.add(normalize(Array.get(array, index), path + "[" + index + "]", allowStringFallback));
        }
        return normalized;
    }
}
