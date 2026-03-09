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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 数学工具模块 - 提供数学运算功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * math.add(1.1, 2.2)      // 3.3
 * math.multiply(3, 4)     // 12.0
 * math.round(3.7)         // 4
 * math.random()           // 0.0 ~ 1.0
 * math.max(10, 20)        // 20.0
 * math.pow(2, 10)         // 1024.0
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class MathModule implements ScriptModule, UtilityFunctions.MathUtils {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_MATH;
    private final Map<String, Object> functions;

    public MathModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("sum", (Function<Collection<Number>, Double>) this::sum);
        functions.put("avg", (Function<Collection<Number>, Double>) this::avg);
        functions.put("add", (BiFunction<Double, Double, Double>) this::add);
        functions.put("subtract", (BiFunction<Double, Double, Double>) this::subtract);
        functions.put("multiply", (BiFunction<Double, Double, Double>) this::multiply);
        functions.put("divide", (BiFunction<Double, Double, Double>) this::divide);
        functions.put("round", (Function<Double, Integer>) this::round);
        functions.put("random", (Supplier<Double>) this::random);
        functions.put("max", (BiFunction<Double, Double, Double>) this::max);
        functions.put("min", (BiFunction<Double, Double, Double>) this::min);
        functions.put("abs", (Function<Double, Double>) this::abs);
        functions.put("pow", (BiFunction<Double, Double, Double>) this::pow);
        functions.put("sqrt", (Function<Double, Double>) this::sqrt);
        functions.put("floor", (Function<Double, Double>) this::floor);
        functions.put("ceil", (Function<Double, Double>) this::ceil);
        functions.put("scale", (TriFunction<Double, Integer, String, Double>) this::scale);
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
        return "Mathematical utility module";
    }

    @Override
    public double add(double a, double b) {
        return BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
    }

    @Override
    public double subtract(double a, double b) {
        return BigDecimal.valueOf(a).subtract(BigDecimal.valueOf(b)).doubleValue();
    }

    @Override
    public double multiply(double a, double b) {
        return BigDecimal.valueOf(a).multiply(BigDecimal.valueOf(b)).doubleValue();
    }

    @Override
    public double divide(double a, double b) {
        if (b == 0) {
            throw new ArithmeticException(LingoNexusConstants.ERROR_DIVISION_BY_ZERO);
        }
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 10, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public double sum(Collection<Number> numbers) {
        // 边界处理：参数为null时返回0.0
        if (Objects.isNull(numbers)) {
            return 0.0;
        }

        // 累加所有元素，转为double计算（兼容int/long/float/double等数值类型）
        double total = 0.0;
        for (Number num : numbers) {
            // 处理集合内的null元素（避免NullPointerException）
            if (Objects.nonNull(num)) {
                total += num.doubleValue();
            }
        }

        // 返回Double类型，匹配测试断言的15.0（而非int 15）
        return total;
    }

    @Override
    public double avg(Collection<Number> numbers) {
        // 边界处理1：集合为null时返回0.0（和sum方法逻辑保持一致）
        if (Objects.isNull(numbers)) {
            return 0.0;
        }

        double total = 0.0; // 有效元素的总和
        int validCount = 0; // 有效元素的个数（排除null）

        // 一次遍历：同时累加总和、统计有效个数（性能最优，仅O(n)时间复杂度）
        for (Number num : numbers) {
            if (Objects.nonNull(num)) {
                total += num.doubleValue();
                validCount++;
            }
        }

        // 边界处理2：无有效元素（空集合/全是null）时返回0.0（避免除以0异常）
        if (validCount == 0) {
            return 0.0;
        }

        // 计算平均值：总和 / 有效元素个数
        return total / validCount;
    }

    @Override
    public Number max(Collection<Number> numbers) {
        // 边界处理1：参数为null → 抛出异常（或返回Double.NaN，根据业务定）
        if (Objects.isNull(numbers)) {
            throw new IllegalArgumentException("Numbers collection cannot be null");
        }
        // 边界处理2：空列表 → 抛出异常（或返回Double.NaN）
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Numbers collection cannot be empty");
        }

        // 初始化最大值：取列表第一个非null元素的double值
        Double maxValue = null;
        for (Number num : numbers) {
            // 跳过列表内的null元素
            if (Objects.isNull(num)) {
                continue;
            }
            double current = num.doubleValue();
            if (maxValue == null) {
                maxValue = current; // 第一个有效元素作为初始最大值
            } else {
                // 复用原有双参数max方法计算当前最大值
                maxValue = this.max(maxValue, current);
            }
        }

        // 边界处理3：列表全是null元素
        if (maxValue == null) {
            throw new IllegalArgumentException("Numbers collection contains no valid number");
        }

        // 返回Double类型，匹配测试断言的30.0（而非int 30）
        return maxValue;
    }

    @Override
    public int round(double value) {
        return (int) Math.round(value);
    }

    @Override
    public double random() {
        return ThreadLocalRandom.current().nextDouble();
    }

    @Override
    public double max(double a, double b) {
        return Math.max(a, b);
    }

    @Override
    public double min(double a, double b) {
        return Math.min(a, b);
    }

    public double abs(double value) {
        return Math.abs(value);
    }

    public double pow(double base, double exponent) {
        return Math.pow(base, exponent);
    }

    public double sqrt(double value) {
        return Math.sqrt(value);
    }

    public double floor(double value) {
        return Math.floor(value);
    }

    public double ceil(double value) {
        return Math.ceil(value);
    }

    /**
     * 设置小数位数
     *
     * @param value 数值
     * @param scale 小数位数
     * @param roundingMode 舍入模式: "UP", "DOWN", "HALF_UP", "HALF_DOWN"
     * @return 处理后的数值
     */
    public double scale(double value, int scale, String roundingMode) {
        RoundingMode mode;
        switch (roundingMode.toUpperCase()) {
            case "UP":
                mode = RoundingMode.UP;
                break;
            case "DOWN":
                mode = RoundingMode.DOWN;
                break;
            case "HALF_DOWN":
                mode = RoundingMode.HALF_DOWN;
                break;
            default:
                mode = RoundingMode.HALF_UP;
        }
        return BigDecimal.valueOf(value).setScale(scale, mode).doubleValue();
    }

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }
}
