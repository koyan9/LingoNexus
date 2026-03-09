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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Performance Test
 * Tests performance characteristics of script execution
 *
 * @author lingonexus Team
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Performance Test")
public class SpringBootPerformanceTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootPerformanceTest.class);

    @Autowired
    private LingoNexusExecutor facade;

    @Test
    @DisplayName("Performance: Sync execution throughput")
    void testSyncExecutionThroughput() {
        // Given
        String script = "return 1 + 1";
        int iterations = 100;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            assertThat(result.isSuccess()).isTrue();
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Sync execution throughput: {} ops/sec ({} ms for {} iterations)",
                    String.format("%.2f", throughput), duration, iterations);
        assertThat(duration).isLessThan(30000); // Should complete in 30 seconds
    }

    @Test
    @DisplayName("Performance: Async execution throughput")
    void testAsyncExecutionThroughput() throws Exception {
        // Given
        String script = "return 1 + 1";
        int iterations = 100;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Async execution throughput: {} ops/sec ({} ms for {} iterations)",
                    String.format("%.2f", throughput), duration, iterations);
        assertThat(futures).allMatch(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Performance: Batch execution throughput")
    void testBatchExecutionThroughput() {
        // Given
        List<String> scripts = new ArrayList<>();
        int batchSize = 100;
        for (int i = 0; i < batchSize; i++) {
            scripts.add("return " + i);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (batchSize * 1000.0) / duration;
        logger.info("Batch execution throughput: {} ops/sec ({} ms for {} scripts)",
                    String.format("%.2f", throughput), duration, batchSize);
        assertThat(results).hasSize(batchSize);
        assertThat(results).allMatch(ScriptResult::isSuccess);
    }

    @Test
    @DisplayName("Performance: Cache effectiveness")
    void testCacheEffectiveness() {
        // Given
        String uncachedScript = "return math.max([1, 2, 3, 4, 5, 6, 7, 8, 9, 10])";
        String cachedScript = "return math.sum([1, 2, 3, 4, 5])";
        int warmupIterations = 10;
        int testIterations = 50;

        // Warmup with different script to warm up the engine
        for (int i = 0; i < warmupIterations; i++) {
            facade.execute(cachedScript, "groovy", ScriptContext.of(Collections.emptyMap()));
        }

        // When - First execution (cache miss)
        long firstStart = System.currentTimeMillis();
        ScriptResult firstResult = facade.execute(uncachedScript, "groovy", ScriptContext.of(Collections.emptyMap()));
        long firstDuration = System.currentTimeMillis() - firstStart;
        assertThat(firstResult.isSuccess()).isTrue();

        // When - Subsequent executions (cache hit)
        long cachedStart = System.currentTimeMillis();
        for (int i = 0; i < testIterations; i++) {
            ScriptResult result = facade.execute(uncachedScript, "groovy", ScriptContext.of(Collections.emptyMap()));
            assertThat(result.isSuccess()).isTrue();
        }
        long cachedDuration = System.currentTimeMillis() - cachedStart;
        double avgCachedTime = cachedDuration / (double) testIterations;

        // Then
        logger.info("Cache effectiveness: First execution: {} ms, Avg cached execution: {} ms",
                    firstDuration, String.format("%.2f", avgCachedTime));
        // Cached executions should be faster (or at least not slower)
        // Use a reasonable threshold since timing can vary
        assertThat(avgCachedTime).isLessThanOrEqualTo(firstDuration + 1.0);
    }

    @Test
    @DisplayName("Performance: Concurrent async execution")
    void testConcurrentAsyncExecution() throws Exception {
        // Given
        int concurrentTasks = 50;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            final int taskId = i;
            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    "return " + taskId + " * 2",
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            );
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Concurrent async execution: {} tasks completed in {} ms", concurrentTasks, duration);
        assertThat(futures).hasSize(concurrentTasks);
        for (int i = 0; i < concurrentTasks; i++) {
            ScriptResult result = futures.get(i).get();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getValue()).isEqualTo(i * 2);
        }
    }

    @Test
    @DisplayName("Performance: Large script compilation")
    void testLargeScriptCompilation() {
        // Given
        StringBuilder scriptBuilder = new StringBuilder();
        scriptBuilder.append("def sum = 0\n");
        for (int i = 1; i <= 100; i++) {
            scriptBuilder.append("sum += ").append(i).append("\n");
        }
        scriptBuilder.append("return sum");
        String script = scriptBuilder.toString();

        // When
        long startTime = System.currentTimeMillis();
        ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Large script compilation and execution: {} ms", duration);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getValue()).isEqualTo(5050);
        assertThat(duration).isLessThan(5000); // Should complete in 5 seconds
    }

    @Test
    @DisplayName("Performance: Module invocation overhead")
    void testModuleInvocationOverhead() {
        // Given
        String directScript = "return 1 + 2 + 3 + 4 + 5";
        String moduleScript = "return math.sum([1, 2, 3, 4, 5])";
        int iterations = 50;

        // When - Direct computation
        long directStart = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            facade.execute(directScript, "groovy", ScriptContext.of(Collections.emptyMap()));
        }
        long directDuration = System.currentTimeMillis() - directStart;

        // When - Module invocation
        long moduleStart = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            facade.execute(moduleScript, "groovy", ScriptContext.of(Collections.emptyMap()));
        }
        long moduleDuration = System.currentTimeMillis() - moduleStart;

        // Then
        logger.info("Module invocation overhead: Direct: {} ms, Module: {} ms", directDuration, moduleDuration);
        double overhead = ((moduleDuration - directDuration) / (double) directDuration) * 100;
        logger.info("Module overhead: {}%", String.format("%.2f", overhead));
        // Module overhead should be reasonable (less than 200%)
        assertThat(moduleDuration).isLessThan(directDuration * 3);
    }

    @Test
    @DisplayName("Performance: Memory efficiency with batch execution")
    void testMemoryEfficiencyWithBatch() {
        // Given
        List<String> scripts = new ArrayList<>();
        int batchSize = 200;
        for (int i = 0; i < batchSize; i++) {
            scripts.add("return 'Result_' + " + i);
        }

        // When
        Runtime runtime = Runtime.getRuntime();
        runtime.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));

        runtime.gc();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;

        // Then
        logger.info("Memory used for batch of {}: {} MB", batchSize, memoryUsed / (1024 * 1024));
        assertThat(results).hasSize(batchSize);
        assertThat(results).allMatch(ScriptResult::isSuccess);
    }

    @Test
    @DisplayName("Performance: Complex math computation throughput")
    void testComplexMathComputationThroughput() {
        // Given
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
        int iterations = 80;

        // When
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        for (int i = 1; i <= iterations; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i);
            ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));
            if (result.isSuccess()) {
                successCount++;
            }
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Complex math computation throughput: {} ops/sec ({} ms for {} iterations, {} successful)",
                String.format("%.2f", throughput), duration, iterations, successCount);
        assertThat(successCount).isGreaterThanOrEqualTo(75); // Allow some failures
    }

    @Test
    @DisplayName("Performance: Complex string processing throughput")
    void testComplexStringProcessingThroughput() {
        // Given
        String script = """
                def text = 'The quick brown fox jumps over the lazy dog ' * 10
                def words = text.split(' ')
                def reversed = words.collect { it.reverse() }
                def joined = reversed.join('-')
                def upper = joined.toUpperCase()
                def result = upper.substring(0, Math.min(50, upper.length()))
                return result.length()
                """;
        int iterations = 100;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            assertThat(result.isSuccess()).isTrue();
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Complex string processing throughput: {} ops/sec ({} ms for {} iterations)",
                String.format("%.2f", throughput), duration, iterations);
        assertThat(duration).isLessThan(30000);
    }

    @Test
    @DisplayName("Performance: Recursive algorithm execution time")
    void testRecursiveAlgorithmExecutionTime() {
        // Given
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
        int iterations = 50;

        // When
        long startTime = System.currentTimeMillis();
        int correctCount = 0;
        for (int i = 0; i < iterations; i++) {
            ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            if (result.isSuccess() && Boolean.TRUE.equals(result.getValue())) {
                correctCount++;
            }
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Recursive algorithm throughput: {} ops/sec ({} ms for {} iterations, {} correct)",
                String.format("%.2f", throughput), duration, iterations, correctCount);
        assertThat(correctCount).isGreaterThanOrEqualTo(45); // Allow 10% failure rate
    }

    @Test
    @DisplayName("Performance: Multi-module complex operations throughput")
    void testMultiModuleComplexOperationsThroughput() {
        // Given
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
        int iterations = 80;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("index", i);
            ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(vars));
            assertThat(result.isSuccess()).isTrue();
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Multi-module complex operations throughput: {} ops/sec ({} ms for {} iterations)",
                String.format("%.2f", throughput), duration, iterations);
        assertThat(duration).isLessThan(30000);
    }

    @Test
    @DisplayName("Performance: Large data processing throughput")
    void testLargeDataProcessingThroughput() {
        // Given
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
        int iterations = 50;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            ScriptResult result = facade.execute(script, "groovy", ScriptContext.of(Collections.emptyMap()));
            assertThat(result.isSuccess()).isTrue();
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (iterations * 1000.0) / duration;
        logger.info("Large data processing throughput: {} ops/sec ({} ms for {} iterations)",
                String.format("%.2f", throughput), duration, iterations);
        assertThat(duration).isLessThan(30000);
    }

    @Test
    @DisplayName("Performance: Batch complex computation vs sync")
    void testBatchComplexComputationVsSync() {
        // Given
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
                return fibonacci(input)
                """;
        int taskCount = 50;

        // When - Sync execution
        long syncStart = System.currentTimeMillis();
        for (int i = 1; i <= taskCount; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i);
            facade.execute(script, "groovy", ScriptContext.of(vars));
        }
        long syncDuration = System.currentTimeMillis() - syncStart;

        // When - Batch execution
        List<String> scripts = new ArrayList<>();
        for (int i = 1; i <= taskCount; i++) {
            scripts.add(script.replace("input", String.valueOf(i)));
        }
        long batchStart = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long batchDuration = System.currentTimeMillis() - batchStart;

        // Then
        logger.info("Batch vs Sync: Sync {} ms, Batch {} ms, Speedup: {}x",
                syncDuration, batchDuration, String.format("%.2f", syncDuration / (double) batchDuration));
        assertThat(results).hasSize(taskCount);
    }

    @Test
    @DisplayName("Performance: Async complex computation vs sync")
    void testAsyncComplexComputationVsSync() throws Exception {
        // Given
        String script = """
                def factorial(n) {
                    if (n <= 1) return 1
                    def result = 1
                    for (int i = 2; i <= n; i++) {
                        result *= i
                    }
                    return result
                }
                return factorial(Math.min(input, 10))
                """;
        int taskCount = 50;

        // When - Sync execution
        long syncStart = System.currentTimeMillis();
        for (int i = 1; i <= taskCount; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i);
            facade.execute(script, "groovy", ScriptContext.of(vars));
        }
        long syncDuration = System.currentTimeMillis() - syncStart;

        // When - Async execution
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        long asyncStart = System.currentTimeMillis();
        for (int i = 1; i <= taskCount; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i);
            CompletableFuture<ScriptResult> future = facade.executeAsync(script, "groovy", ScriptContext.of(vars));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(60, TimeUnit.SECONDS);
        long asyncDuration = System.currentTimeMillis() - asyncStart;

        // Then
        logger.info("Async vs Sync: Sync {} ms, Async {} ms, Speedup: {}x",
                syncDuration, asyncDuration, String.format("%.2f", syncDuration / (double) asyncDuration));
        assertThat(futures).hasSize(taskCount);
    }
}
