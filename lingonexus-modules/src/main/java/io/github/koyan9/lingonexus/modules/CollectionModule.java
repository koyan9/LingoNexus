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

/**
 * 集合工具模块 - 提供集合操作功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * col.size([1, 2, 3])          // 3
 * col.isEmpty([])               // true
 * col.contains([1, 2, 3], 2)   // true
 * col.toList(someCollection)    // ArrayList
 * col.toMap(someMap)            // HashMap
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class CollectionModule implements ScriptModule, UtilityFunctions.CollectionUtils {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_COLLECTION;
    private final Map<String, Object> functions;

    public CollectionModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    @SuppressWarnings("unchecked")
    private void initFunctions() {
        functions.put("size", (Function<Collection<?>, Integer>) this::size);
        functions.put("isEmpty", (Function<Collection<?>, Boolean>) this::isEmpty);
        functions.put("isNotEmpty", (Function<Collection<?>, Boolean>) this::isNotEmpty);
        functions.put("contains", (BiFunction<Collection<?>, Object, Boolean>) this::contains);
        functions.put("toList", (Function<Collection<?>, List<?>>) this::toList);
        functions.put("toMap", (Function<Map<?, ?>, Map<?, ?>>) this::toMap);
        functions.put("first", (Function<List<?>, Object>) this::first);
        functions.put("last", (Function<List<?>, Object>) this::last);
        functions.put("subList", (TriFunction<List<?>, Integer, Integer, List<?>>) this::subList);
        functions.put("flatten", (Function<Collection<? extends Collection<?>>, List<?>>) this::flatten);
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
        return "Collection utility module for list and map operations";
    }

    @Override
    public int size(Collection<?> collection) {
        return collection == null ? 0 : collection.size();
    }

    @Override
    public boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    @Override
    public boolean contains(Collection<?> collection, Object element) {
        return collection != null && collection.contains(element);
    }

    @Override
    public List<?> toList(Collection<?> collection) {
        if (collection == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(collection);
    }

    @Override
    public Map<?, ?> toMap(Map<?, ?> map) {
        if (map == null) {
            return new HashMap<>();
        }
        return new HashMap<>(map);
    }

    public Object first(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }

    public Object last(List<?> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    public List<?> subList(List<?> list, int fromIndex, int toIndex) {
        if (list == null) {
            return new ArrayList<>();
        }
        int size = list.size();
        int from = Math.max(0, fromIndex);
        int to = Math.min(size, toIndex);
        if (from >= to) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(from, to));
    }

    public List<?> flatten(Collection<? extends Collection<?>> collections) {
        if (collections == null) {
            return new ArrayList<>();
        }
        List<Object> result = new ArrayList<>();
        for (Collection<?> col : collections) {
            if (col != null) {
                result.addAll(col);
            }
        }
        return result;
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
