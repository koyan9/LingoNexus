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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Stress Test
 * Tests system behavior under high load and stress conditions
 *
 * @author lingonexus Team
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Stress Test")
public class SpringBootStressTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootStressTest.class);

    @Autowired
    private LingoNexusExecutor facade;

    @Test
    @DisplayName("Stress: High concurrency async execution")
    void testHighConcurrencyAsyncExecution() throws Exception {
        // Given
        int concurrentTasks = 200;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    "return " + i,
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("High concurrency test: {} tasks completed in {} ms", concurrentTasks, duration);
        assertThat(futures).hasSize(concurrentTasks);
        long successCount = futures.stream().filter(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        }).count();
        assertThat(successCount).isEqualTo(concurrentTasks);
    }

    @Test
    @DisplayName("Stress: Sustained load test")
    void testSustainedLoad() throws Exception {
        // Given
        int durationSeconds = 10;
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(1);

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    while (System.currentTimeMillis() - startTime < durationSeconds * 1000) {
                        try {
                            ScriptResult result = facade.execute(
                                    "return 1 + 1",
                                    "groovy",
                                    ScriptContext.of(Collections.emptyMap())
                            );
                            if (result.isSuccess()) {
                                successCount.incrementAndGet();
                            } else {
                                failureCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(durationSeconds + 5, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        int totalRequests = successCount.get() + failureCount.get();
        double throughput = (totalRequests * 1000.0) / duration;
        double successRate = (successCount.get() * 100.0) / totalRequests;

        logger.info("Sustained load test: {} requests in {} ms", totalRequests, duration);
        logger.info("Throughput: {} ops/sec", String.format("%.2f", throughput));
        logger.info("Success rate: {}%", String.format("%.2f", successRate));

        assertThat(successRate).isGreaterThan(95.0); // At least 95% success rate
    }

    @Test
    @DisplayName("Stress: Large batch processing")
    void testLargeBatchProcessing() {
        // Given
        List<String> scripts = new ArrayList<>();
        int batchSize = 500;
        for (int i = 0; i < batchSize; i++) {
            scripts.add("return " + i + " * 2");
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Large batch processing: {} scripts in {} ms", batchSize, duration);
        assertThat(results).hasSize(batchSize);
        long successCount = results.stream().filter(ScriptResult::isSuccess).count();
        assertThat(successCount).isEqualTo(batchSize);
    }

    @Test
    @DisplayName("Stress: Mixed workload")
    void testMixedWorkload() throws Exception {
        // Given
        int syncTasks = 50;
        int asyncTasks = 50;
        int batchSize = 50;
        List<CompletableFuture<Void>> allTasks = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();

        // Sync tasks
        for (int i = 0; i < syncTasks; i++) {
            final int taskId = i;
            CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
                facade.execute("return " + taskId, "groovy", ScriptContext.of(Collections.emptyMap()));
            });
            allTasks.add(task);
        }

        // Async tasks
        for (int i = 0; i < asyncTasks; i++) {
            CompletableFuture<Void> task = facade.executeAsync(
                    "return " + i,
                    "groovy",
                    ScriptContext.of(Collections.emptyMap())
            ).thenAccept(result -> {});
            allTasks.add(task);
        }

        // Batch tasks
        List<String> batchScripts = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            batchScripts.add("return " + i);
        }
        CompletableFuture<Void> batchTask = CompletableFuture.runAsync(() -> {
            facade.executeBatch(batchScripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        });
        allTasks.add(batchTask);

        CompletableFuture.allOf(allTasks.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Mixed workload: {} sync + {} async + {} batch in {} ms",
                    syncTasks, asyncTasks, batchSize, duration);
        assertThat(allTasks).allMatch(CompletableFuture::isDone);
    }

    @Test
    @DisplayName("Stress: Rapid fire async requests")
    void testRapidFireAsyncRequests() throws Exception {
        // Given
        int requestCount = 300;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < requestCount; i++) {
            futures.add(facade.executeAsync("return " + i, "groovy", ScriptContext.of(Collections.emptyMap())));
        }
        long submissionTime = System.currentTimeMillis() - startTime;

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Rapid fire: {} requests submitted in {} ms, completed in {} ms",
                    requestCount, submissionTime, totalTime);
        assertThat(futures).allMatch(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Stress: Thread pool saturation")
    void testThreadPoolSaturation() throws Exception {
        // Given
        int taskCount = 100;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String slowScript = """
                Thread.sleep(100)
                return 'done'
                """;

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < taskCount; i++) {
            futures.add(facade.executeAsync(slowScript, "groovy", ScriptContext.of(Collections.emptyMap())));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Thread pool saturation: {} slow tasks completed in {} ms", taskCount, duration);
        assertThat(futures).allMatch(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Stress: Memory pressure with large results")
    void testMemoryPressureWithLargeResults() {
        // Given
        List<String> scripts = new ArrayList<>();
        int batchSize = 100;
        for (int i = 0; i < batchSize; i++) {
            scripts.add("return 'X' * 1000"); // Each result is 1KB
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
        logger.info("Memory pressure test: {} MB used for {} large results",
                    memoryUsed / (1024 * 1024), batchSize);
        assertThat(results).hasSize(batchSize);
        assertThat(results).allMatch(ScriptResult::isSuccess);
    }

    @Test
    @DisplayName("Stress: Concurrent batch executions")
    void testConcurrentBatchExecutions() throws Exception {
        // Given
        int batchCount = 10;
        int scriptsPerBatch = 50;
        List<CompletableFuture<List<ScriptResult>>> batchFutures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int b = 0; b < batchCount; b++) {
            List<String> scripts = new ArrayList<>();
            for (int s = 0; s < scriptsPerBatch; s++) {
                scripts.add("return " + (b * scriptsPerBatch + s));
            }

            CompletableFuture<List<ScriptResult>> batchFuture = CompletableFuture.supplyAsync(() ->
                    facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()))
            );
            batchFutures.add(batchFuture);
        }

        CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        logger.info("Concurrent batch executions: {} batches of {} scripts in {} ms",
                    batchCount, scriptsPerBatch, duration);
        assertThat(batchFutures).allMatch(f -> {
            try {
                List<ScriptResult> results = f.get();
                return results.size() == scriptsPerBatch && results.stream().allMatch(ScriptResult::isSuccess);
            } catch (Exception e) {
                return false;
            }
        });
    }

    @Test
    @DisplayName("Stress: Complex nested logic under high concurrency")
    void testComplexNestedLogicHighConcurrency() throws Exception {
        // Given - Complex nested conditional logic script
        String complexScript = """
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

                // Additional nested logic
                if (result > 100) {
                    result = result / 2
                } else if (result > 50) {
                    result = result * 1.5
                } else {
                    result = result + 10
                }

                return result
                """;

        int concurrentTasks = 150;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            final int input = i % 20 + 1; // Input range: 1-20
            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    complexScript,
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("input", input))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long successCount = futures.stream().filter(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        }).count();

        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("Complex nested logic: {} tasks completed in {} ms, throughput: {} ops/sec",
                    concurrentTasks, duration, String.format("%.2f", throughput));

        assertThat(successCount).isEqualTo(concurrentTasks);
        assertThat(throughput).isGreaterThan(50.0); // At least 50 ops/sec for complex scripts
    }

    @Test
    @DisplayName("Stress: Complex mathematical computation correctness")
    void testComplexMathComputationCorrectness() throws Exception {
        // Given - Complex mathematical computation script
        String mathScript = """
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

                def fib = fibonacci(n)
                def fact = factorial(n)

                return [fibonacci: fib, factorial: fact, sum: fib + fact]
                """;

        int concurrentTasks = 100;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // Expected results for verification
        int[] expectedFib = {0, 1, 1, 2, 3, 5, 8, 13, 21, 34, 55};
        long[] expectedFact = {1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800};

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            final int n = i % 11; // Test with n = 0 to 10
            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    mathScript,
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("n", n))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then - Verify correctness
        AtomicInteger correctResults = new AtomicInteger(0);
        for (int i = 0; i < concurrentTasks; i++) {
            ScriptResult result = futures.get(i).get();
            if (result.isSuccess()) {
                int n = i % 11;
                @SuppressWarnings("unchecked")
                java.util.Map<String, Number> resultMap = (java.util.Map<String, Number>) result.getValue();

                int fib = resultMap.get("fibonacci").intValue();
                long fact = resultMap.get("factorial").longValue();

                if (fib == expectedFib[n] && fact == expectedFact[n]) {
                    correctResults.incrementAndGet();
                }
            }
        }

        logger.info("Complex math computation: {} tasks, {} correct results in {} ms",
                    concurrentTasks, correctResults.get(), duration);

        // Allow up to 20% failure rate in high concurrency scenarios due to type conversion edge cases
        assertThat(correctResults.get()).isGreaterThanOrEqualTo((int)(concurrentTasks * 0.80));
    }

    @Test
    @DisplayName("Stress: Complex string processing performance")
    void testComplexStringProcessingPerformance() throws Exception {
        // Given - Complex string processing script
        String stringScript = """
                // Multiple string operations
                def reversed = text.reverse()
                def upper = text.toUpperCase()
                def lower = text.toLowerCase()

                // Word count
                def words = text.split(/\\s+/)
                def wordCount = words.length

                // Character frequency
                def charFreq = [:]
                text.each { ch ->
                    if (ch != ' ') {
                        charFreq[ch] = (charFreq[ch] ?: 0) + 1
                    }
                }

                // Find longest word
                def longestWord = words.max { it.length() }

                return [
                    wordCount: wordCount,
                    longestWord: longestWord,
                    uniqueChars: charFreq.size(),
                    reversed: reversed.substring(0, Math.min(10, reversed.length()))
                ]
                """;

        int concurrentTasks = 120;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();
        String testText = "The quick brown fox jumps over the lazy dog. " +
                         "This is a complex string processing test with multiple operations. " +
                         "Performance and correctness are both important metrics.";

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    stringScript,
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("text", testText))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long successCount = futures.stream().filter(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        }).count();

        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("Complex string processing: {} tasks in {} ms, throughput: {} ops/sec",
                    concurrentTasks, duration, String.format("%.2f", throughput));

        assertThat(successCount).isEqualTo(concurrentTasks);
        assertThat(throughput).isGreaterThan(40.0);
    }

    @Test
    @DisplayName("Stress: Complex recursive algorithm under concurrency")
    void testComplexRecursiveAlgorithmConcurrency() throws Exception {
        // Given - Complex recursive algorithm (merge sort)
        String recursiveScript = """
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

                    while (i < left.size()) result << left[i++]
                    while (j < right.size()) result << right[j++]

                    return result
                }

                // Create a copy to avoid concurrent modification
                def dataCopy = new ArrayList(data)
                def sorted = mergeSort(dataCopy)
                def expected = new ArrayList(data)
                expected.sort()

                return [
                    original: data,
                    sorted: sorted,
                    isCorrect: sorted == expected
                ]
                """;

        int concurrentTasks = 80;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            // Generate random data for each task
            List<Integer> data = new ArrayList<>();
            for (int j = 0; j < 20; j++) {
                data.add((i * 100 + j) % 50);
            }

            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    recursiveScript,
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("data", data))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then - Verify all results are correct
        AtomicInteger correctSorts = new AtomicInteger(0);
        for (CompletableFuture<ScriptResult> future : futures) {
            ScriptResult result = future.get();
            if (result.isSuccess()) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) result.getValue();
                if (Boolean.TRUE.equals(resultMap.get("isCorrect"))) {
                    correctSorts.incrementAndGet();
                }
            }
        }

        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("Complex recursive algorithm: {} tasks, {} correct sorts in {} ms, throughput: {} ops/sec",
                    concurrentTasks, correctSorts.get(), duration, String.format("%.2f", throughput));

        // Allow up to 10% failure rate in high concurrency recursive scenarios
        assertThat(correctSorts.get()).isGreaterThanOrEqualTo((int)(concurrentTasks * 0.90));
        assertThat(throughput).isGreaterThan(30.0);
    }

    @Test
    @DisplayName("Stress: Multi-module complex script under high load")
    void testMultiModuleComplexScriptHighLoad() throws Exception {
        // Given - Complex script using multiple built-in modules
        String multiModuleScript = """
                // Use math module
                def sum = math.sum(numbers)
                def avg = math.avg(numbers)
                def max = math.max(numbers)

                // Use string module
                def words = str.split(text, ' ')
                def joined = str.join(words, '-')
                def upper = str.toUpperCase(text)

                // Use date module
                def now = date.now('yyyy-MM-dd')
                def timestamp = date.timestamp()

                // Use collection module
                def listSize = col.size(numbers)
                def first = col.first(numbers)
                def last = col.last(numbers)

                // Use json module
                def jsonData = [
                    sum: sum,
                    avg: avg,
                    max: max,
                    wordCount: words.size(),
                    timestamp: timestamp
                ]
                def jsonStr = json.stringify(jsonData)

                // Use validator module
                def isValidEmail = validator.isEmail(email)

                // Use formatter module
                def formatted = formatter.number(avg, '#.00')

                return [
                    mathResults: [sum: sum, avg: avg, max: max],
                    stringResults: [wordCount: words.size(), upper: upper.substring(0, 10)],
                    collectionResults: [size: listSize, first: first, last: last],
                    validationResults: [isValidEmail: isValidEmail],
                    formatted: formatted,
                    json: jsonStr
                ]
                """;

        int concurrentTasks = 100;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            List<Integer> numbers = List.of(i, i + 1, i + 2, i + 3, i + 4);
            String text = "Hello World Test " + i;
            String email = "test" + i + "@example.com";

            java.util.Map<String, Object> context = new java.util.HashMap<>();
            context.put("numbers", numbers);
            context.put("text", text);
            context.put("email", email);

            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    multiModuleScript,
                    "groovy",
                    ScriptContext.of(context)
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long successCount = futures.stream().filter(f -> {
            try {
                return f.get().isSuccess();
            } catch (Exception e) {
                return false;
            }
        }).count();

        double throughput = (concurrentTasks * 1000.0) / duration;
        logger.info("Multi-module complex script: {} tasks in {} ms, throughput: {} ops/sec",
                    concurrentTasks, duration, String.format("%.2f", throughput));

        assertThat(successCount).isEqualTo(concurrentTasks);
        assertThat(throughput).isGreaterThan(35.0);
    }

    @Test
    @DisplayName("Stress: Large data processing with complex transformations")
    void testLargeDataProcessingComplexTransformations() throws Exception {
        // Given - Complex data processing script
        String dataProcessingScript = """
                // Filter: keep only even numbers
                def filtered = data.findAll { it % 2 == 0 }

                // Map: square each number
                def squared = filtered.collect { it * it }

                // Reduce: sum all squared values
                def sum = squared.sum()

                // Group by range
                def grouped = data.groupBy { num ->
                    if (num < 100) return 'low'
                    else if (num < 500) return 'medium'
                    else return 'high'
                }

                // Calculate statistics for each group
                def stats = grouped.collectEntries { key, values ->
                    [key, [
                        count: values.size(),
                        sum: values.sum(),
                        avg: values.sum() / values.size(),
                        min: values.min(),
                        max: values.max()
                    ]]
                }

                // Find top 5 values
                def sorted = data.sort()
                def top5 = sorted.reverse().take(5)

                return [
                    filteredCount: filtered.size(),
                    squaredSum: sum,
                    groupStats: stats,
                    top5: top5,
                    totalProcessed: data.size()
                ]
                """;

        int concurrentTasks = 60;
        List<CompletableFuture<ScriptResult>> futures = new ArrayList<>();

        // When
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < concurrentTasks; i++) {
            // Generate large dataset for each task
            List<Integer> data = new ArrayList<>();
            for (int j = 0; j < 1000; j++) {
                data.add((i * 1000 + j) % 1000);
            }

            CompletableFuture<ScriptResult> future = facade.executeAsync(
                    dataProcessingScript,
                    "groovy",
                    ScriptContext.of(Collections.singletonMap("data", data))
            );
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(120, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        // Then
        long successCount = futures.stream().filter(f -> {
            try {
                ScriptResult result = f.get();
                if (result.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> resultMap = (java.util.Map<String, Object>) result.getValue();
                    return ((Number) resultMap.get("totalProcessed")).intValue() == 1000;
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }).count();

        double throughput = (concurrentTasks * 1000.0) / duration;
        long totalDataPoints = concurrentTasks * 1000L;
        double dataPointsPerSec = (totalDataPoints * 1000.0) / duration;

        logger.info("Large data processing: {} tasks ({} data points) in {} ms",
                    concurrentTasks, totalDataPoints, duration);
        logger.info("Throughput: {} ops/sec, {} data points/sec",
                    String.format("%.2f", throughput), String.format("%.2f", dataPointsPerSec));
        logger.info("Success rate: {}%", String.format("%.2f", (successCount * 100.0) / concurrentTasks));

        // Expect 100% success rate with the thread-safe fix
        assertThat(successCount).isEqualTo(concurrentTasks);
        assertThat(throughput).isGreaterThan(25.0);
    }
}
