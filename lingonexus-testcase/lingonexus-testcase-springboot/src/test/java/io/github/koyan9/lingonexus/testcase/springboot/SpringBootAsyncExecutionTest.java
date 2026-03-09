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

package io.github.koyan9.lingonexus.testcase.springboot;

import io.github.koyan9.lingonexus.api.context.ScriptContext;
import io.github.koyan9.lingonexus.api.facade.LingoNexusExecutor;
import io.github.koyan9.lingonexus.api.result.ScriptResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Async Execution Test
 * Tests asynchronous script execution functionality
 *
 * @author lingonexus Team
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Async Execution Test")
public class SpringBootAsyncExecutionTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootAsyncExecutionTest.class);

    @Autowired
    private LingoNexusExecutor facade;

    @Test
    @DisplayName("Should execute simple script asynchronously")
    void shouldExecuteSimpleScriptAsync() throws Exception {
        // Given
        String script = "return 1 + 2";

        // When
        CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
        ScriptResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should execute script with variables asynchronously")
    void shouldExecuteScriptWithVariablesAsync() throws Exception {
        // Given
        String script = "return x * y";
        Map<String, Object> vars = new HashMap<>();
        vars.put("x", 6);
        vars.put("y", 7);

        // When
        CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(vars));
        ScriptResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(42);
    }

    @Test
    @DisplayName("Should execute multiple scripts asynchronously in parallel")
    void shouldExecuteMultipleScriptsAsyncInParallel() throws Exception {
        // Given
        int scriptCount = 10;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        for (int i = 0; i < scriptCount; i++) {
            String script = "return " + i + " * 2";
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            futures.add(future);
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(10, TimeUnit.SECONDS);

        // Then
        for (int i = 0; i < scriptCount; i++) {
            ScriptResult result = futures.get(i).get();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(i * 2);
        }
    }

    @Test
    @DisplayName("Should execute async script with module")
    void shouldExecuteAsyncScriptWithModule() throws Exception {
        // Given
        String script = "return math.max([10, 20, 30, 5])";

        // When
        CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
        ScriptResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(30.0);
    }

    @Test
    @DisplayName("Should execute JavaScript asynchronously")
    void shouldExecuteJavaScriptAsync() throws Exception {
        // Given
        String script = "a + b";
        Map<String, Object> vars = new HashMap<>();
        vars.put("a", 100);
        vars.put("b", 200);

        // When
        CompletableFuture<ScriptResult> future = facade.executeAsync(script, "javascript", ScriptContext.of(vars));
        ScriptResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(300);
    }

    @Test
    @DisplayName("Should handle async execution with callback")
    void shouldHandleAsyncExecutionWithCallback() throws Exception {
        // Given
        String script = "return 'Hello, Async!'";
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger callbackCount = new AtomicInteger(0);

        // When
        CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
        future.thenAccept(result -> {
            callbackCount.incrementAndGet();
            latch.countDown();
        });

        // Then
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(callbackCount.get()).isEqualTo(1);
        assertThat(future.get().getValue()).isEqualTo("Hello, Async!");
    }

    @Test
    @DisplayName("Should execute async scripts with different languages concurrently")
    void shouldExecuteAsyncScriptsWithDifferentLanguages() throws Exception {
        // Given
        CompletableFuture<ScriptResult> groovyFuture = facade.executeAsync("return 'Groovy'", "groovy", ScriptContext.of(Collections.emptyMap()));
        CompletableFuture<ScriptResult> jsFuture = facade.executeAsync("'JavaScript'", "javascript", ScriptContext.of(Collections.emptyMap()));

        // When
        CompletableFuture.allOf(groovyFuture, jsFuture).get(10, TimeUnit.SECONDS);

        // Then
        assertThat(groovyFuture.get().getValue()).isEqualTo("Groovy");
        assertThat(jsFuture.get().getValue()).isEqualTo("JavaScript");
    }

    @Test
    @DisplayName("Should execute complex async computation")
    void shouldExecuteComplexAsyncComputation() throws Exception {
        // Given
        String script = """
                def sum = 0
                for (int i = 1; i <= 100; i++) {
                    sum += i
                }
                return sum
                """;

        // When
        CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
        ScriptResult result = future.get(5, TimeUnit.SECONDS);

        // Then
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(5050);
    }

    @Test
    @DisplayName("Should execute high concurrency async complex math computations")
    void shouldExecuteHighConcurrencyAsyncComplexMathComputations() throws Exception {
        // Given
        int concurrentTasks = 120;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String script = """
                def fibonacci(n) {
                    if (n <= 1) return n
                    def a = 0, b = 1
                    for (int i = 2; i <= n; i++) {
                        def temp = a + b
                        a = b
                        b = temp
                    }
                    return b
                }
                def factorial(n) {
                    if (n <= 1) return 1
                    def result = 1
                    for (int i = 2; i <= n; i++) {
                        result *= i
                    }
                    return result
                }
                return fibonacci(input) + factorial(Math.min(input, 10))
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= concurrentTasks; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i);
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(vars));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("High concurrency async complex math: {} tasks in {} ms, throughput: {} ops/sec",
                concurrentTasks, duration, String.format("%.2f", throughput));

        long successCount = futures.stream()
                .filter(f -> {
                    try {
                        return f.get().isSuccess();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
        assertThat(successCount).isGreaterThanOrEqualTo(110); // Allow ~8% failure rate
    }

    @Test
    @DisplayName("Should execute high concurrency async complex string processing")
    void shouldExecuteHighConcurrencyAsyncComplexStringProcessing() throws Exception {
        // Given
        int concurrentTasks = 100;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String script = """
                def text = 'The quick brown fox jumps over the lazy dog ' * 10
                def words = text.split(' ')
                def reversed = words.collect { it.reverse() }
                def joined = reversed.join('-')
                def upper = joined.toUpperCase()
                def result = upper.substring(0, Math.min(50, upper.length()))
                return result.length()
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("High concurrency async string processing: {} tasks in {} ms, throughput: {} ops/sec",
                concurrentTasks, duration, String.format("%.2f", throughput));

        assertThat(futures).allMatch(f -> {
            try {
                ScriptResult result = f.get();
                return result.isSuccess() && (Integer) result.getValue() == 50;
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Should execute high concurrency async multi-module operations")
    void shouldExecuteHighConcurrencyAsyncMultiModuleOperations() throws Exception {
        // Given
        int concurrentTasks = 80;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String script = """
                def numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
                def sum = math.sum(numbers)
                def avg = math.avg(numbers)
                def max = math.max(numbers)

                def text = 'test-string-' + index
                def reversed = str.reverse(text)
                def upper = str.toUpperCase(reversed)

                def list = col.toList(numbers)
                def size = col.size(list)

                def isValid = validator.notNull(sum) && validator.isNumber(String.valueOf(avg))

                return [sum: sum, avg: avg, max: max, size: size, valid: isValid]
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("index", i);
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(vars));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("High concurrency async multi-module: {} tasks in {} ms, throughput: {} ops/sec",
                concurrentTasks, duration, String.format("%.2f", throughput));

        assertThat(futures).allMatch(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Should execute high concurrency async large data processing")
    void shouldExecuteHighConcurrencyAsyncLargeDataProcessing() throws Exception {
        // Given
        int concurrentTasks = 60;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String script = """
                def data = []
                for (int j = 0; j < 500; j++) {
                    data << [id: j, value: j * 2, name: 'item_' + j]
                }

                def filtered = data.findAll { it.value > 100 }
                def mapped = filtered.collect { it.value }
                def sum = mapped.sum()

                return [total: data.size(), filtered: filtered.size(), sum: sum]
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("High concurrency async large data: {} tasks in {} ms, throughput: {} ops/sec",
                concurrentTasks, duration, String.format("%.2f", throughput));

        assertThat(futures).allMatch(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Should execute high concurrency async recursive algorithms")
    void shouldExecuteHighConcurrencyAsyncRecursiveAlgorithms() throws Exception {
        // Given
        int concurrentTasks = 50;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String script = """
                def data = (1..50).collect { (int)(Math.random() * 100) }

                def mergeSort(list) {
                    if (list.size() <= 1) return list
                    def mid = list.size() / 2
                    def left = mergeSort(list[0..<mid])
                    def right = mergeSort(list[mid..<list.size()])
                    return merge(left, right)
                }

                def merge(left, right) {
                    def result = []
                    def i = 0, j = 0
                    while (i < left.size() && j < right.size()) {
                        if (left[i] <= right[j]) {
                            result << left[i++]
                        } else {
                            result << right[j++]
                        }
                    }
                    result.addAll(left[i..<left.size()])
                    result.addAll(right[j..<right.size()])
                    return result
                }

                def dataCopy = new ArrayList(data)
                def sorted = mergeSort(dataCopy)
                def expected = new ArrayList(data)
                expected.sort()

                return sorted == expected
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("High concurrency async recursive: {} tasks in {} ms, throughput: {} ops/sec",
                concurrentTasks, duration, String.format("%.2f", throughput));

        long correctCount = futures.stream()
                .filter(f -> {
                    try {
                        ScriptResult result = f.get();
                        return result.isSuccess() && Boolean.TRUE.equals(result.getValue());
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
        assertThat(correctCount).isGreaterThanOrEqualTo(45); // Allow 10% failure rate
    }

    @Test
    @DisplayName("Should execute extreme concurrency async with complex nested logic")
    void shouldExecuteExtremeConcurrencyAsyncWithComplexNestedLogic() throws Exception {
        // Given
        int concurrentTasks = 150;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String script = """
                def calculate(n) {
                    if (n <= 1) return 1
                    def result = 0
                    for (int i = 1; i <= n; i++) {
                        if (i % 2 == 0) {
                            result += i * 2
                        } else {
                            result += i * 3
                        }
                    }
                    return result
                }
                def result = calculate(input)
                if (result > 100) {
                    result = result / 2
                }
                if (result % 3 == 0) {
                    result = result * 1.5
                }
                return result
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 1; i <= concurrentTasks; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i % 50 + 1);
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(vars));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("Extreme concurrency async nested logic: {} tasks in {} ms, throughput: {} ops/sec",
                concurrentTasks, duration, String.format("%.2f", throughput));

        long successCount = futures.stream()
                .filter(f -> {
                    try {
                        return f.get().isSuccess();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();
        assertThat(successCount).isGreaterThanOrEqualTo(140); // Allow ~7% failure rate
    }
}
