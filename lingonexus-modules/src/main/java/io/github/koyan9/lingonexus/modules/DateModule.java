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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQueries;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 日期时间工具模块 - 提供日期时间处理功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * date.now("yyyy-MM-dd")           // "2026-02-02"
 * date.timestamp()                  // 1738483200000
 * date.format(1738483200000, "yyyy-MM-dd HH:mm:ss")
 * date.parse("2026-02-02", "yyyy-MM-dd")
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class DateModule implements ScriptModule, UtilityFunctions.Date {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_DATE;
    private final Map<String, Object> functions;

    public DateModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("now", (Function<String, String>) this::now);
        functions.put("timestamp", (Supplier<Long>) this::timestamp);
        functions.put("format", (BiFunction<Long, String, String>) this::format);
        functions.put("parse", (BiFunction<String, String, Long>) this::parse);
        functions.put("addDays", (BiFunction<Long, Integer, Long>) this::addDays);
        functions.put("addHours", (BiFunction<Long, Integer, Long>) this::addHours);
        functions.put("daysBetween", (BiFunction<Long, Long, Long>) this::daysBetween);
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
        return "Date and time utility module";
    }

    @Override
    public String now(String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return LocalDateTime.now().format(formatter);
    }

    @Override
    public long timestamp() {
        return System.currentTimeMillis();
    }

    @Override
    public String format(long timestamp, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return dateTime.format(formatter);
    }

    @Override
    public long parse(String dateStr, String format) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        TemporalAccessor parsed = formatter.parse(dateStr);
        LocalDateTime dateTime;
        if (parsed.query(TemporalQueries.localTime()) != null) {
            dateTime = LocalDateTime.from(parsed);
        } else {
            dateTime = LocalDate.from(parsed).atStartOfDay();
        }
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public long addDays(long timestamp, int days) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return dateTime.plusDays(days).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public long addHours(long timestamp, int hours) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        return dateTime.plusHours(hours).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public long daysBetween(long timestamp1, long timestamp2) {
        long diffInMillis = Math.abs(timestamp2 - timestamp1);
        return diffInMillis / (24 * 60 * 60 * 1000);
    }
}
