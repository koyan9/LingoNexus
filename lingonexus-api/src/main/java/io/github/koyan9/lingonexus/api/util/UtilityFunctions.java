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

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 工具函数接口，提供内置的工具方法
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public interface UtilityFunctions {

    /**
     * JSON 相关工具函数
     */
    interface Json {
        String toJson(Object obj);

        String stringify(Object obj);

        Object fromJson(String json);

        Object parse(String json);

        Object parsePath(String json, String path);
    }

    /**
     * 字符串相关工具函数
     */
    interface StringUtils {

        String reverse(String str);

        boolean isBlank(String str);

        boolean isNotBlank(String str);

        String trim(String str);

        int length(String str);

        String substring(String str, int start, int end);

        boolean matches(String str, String regex);

        List<String> split(String str, String delimiter);

        String join(Collection<String> strings, String delimiter);
    }

    /**
     * 日期时间相关工具函数
     */
    interface Date {
        String now(String format);

        long timestamp();

        String format(long timestamp, String format);

        long parse(String dateStr, String format);
    }

    /**
     * 数学相关工具函数
     */
    interface MathUtils {

        double avg(Collection<Number> numbers);

        double sum(Collection<Number> numbers);

        Number max(Collection<Number> numbers);

        double add(double a, double b);

        double subtract(double a, double b);

        double multiply(double a, double b);

        double divide(double a, double b);

        int round(double value);

        double random();

        double max(double a, double b);

        double min(double a, double b);
    }

    /**
     * 集合相关工具函数
     */
    interface CollectionUtils {
        int size(Collection<?> collection);

        boolean isEmpty(Collection<?> collection);

        boolean contains(Collection<?> collection, Object element);

        List<?> toList(Collection<?> collection);

        Map<?, ?> toMap(Map<?, ?> map);
    }

    /**
     * 类型转换相关工具函数
     */
    interface Convert {
        String toString(Object value);

        Integer toInteger(Object value);

        Long toLong(Object value);

        Double toDouble(Object value);

        Boolean toBoolean(Object value);
    }
}
