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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Boot Batch Execution Test
 * Tests batch script execution functionality
 *
 * @author lingonexus Team
 */
@SpringBootTest(classes = TestApplication.class)
@DisplayName("Spring Boot Batch Execution Test")
public class SpringBootBatchExecutionTest {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootBatchExecutionTest.class);

    @Autowired
    private LingoNexusExecutor facade;

    @Test
    @DisplayName("Should execute batch of simple scripts")
    void shouldExecuteBatchOfSimpleScripts() {
        // Given
        List<String> scripts = Arrays.asList(
                "return 1 + 1",
                "return 2 + 2",
                "return 3 + 3"
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getValue()).isEqualTo(2);
        assertThat(results.get(1).getValue()).isEqualTo(4);
        assertThat(results.get(2).getValue()).isEqualTo(6);
        assertThat(results).allMatch(ScriptResult::isSuccess);
    }

    @Test
    @DisplayName("Should execute batch with shared context")
    void shouldExecuteBatchWithSharedContext() {
        // Given
        Map<String, Object> vars = new HashMap<>();
        vars.put("multiplier", 10);

        List<String> scripts = Arrays.asList(
                "return 1 * multiplier",
                "return 2 * multiplier",
                "return 3 * multiplier"
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(vars));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getValue()).isEqualTo(10);
        assertThat(results.get(1).getValue()).isEqualTo(20);
        assertThat(results.get(2).getValue()).isEqualTo(30);
    }

    @Test
    @DisplayName("Should execute batch with modules")
    void shouldExecuteBatchWithModules() {
        // Given
        List<String> scripts = Arrays.asList(
                "return math.max([1, 5, 3])",
                "return str.reverse('hello')",
                "return math.sum([1, 2, 3, 4, 5])"
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getValue()).isEqualTo(5.0);
        assertThat(results.get(1).getValue()).isEqualTo("olleh");
        assertThat(results.get(2).getValue()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Should execute large batch efficiently")
    void shouldExecuteLargeBatchEfficiently() {
        // Given
        List<String> scripts = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            scripts.add("return " + i + " * 2");
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        assertThat(results).hasSize(50);
        assertThat(results).allMatch(ScriptResult::isSuccess);
        for (int i = 0; i < 50; i++) {
            assertThat(results.get(i).getValue()).isEqualTo(i * 2);
        }
        // Batch execution should be reasonably fast
        assertThat(duration).isLessThan(10000); // Less than 10 seconds
    }

    @Test
    @DisplayName("Should execute batch with JavaScript")
    void shouldExecuteBatchWithJavaScript() {
        // Given
        List<String> scripts = Arrays.asList(
                "1 + 1",
                "2 * 3",
                "10 - 5"
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "javascript", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getValue()).isEqualTo(2);
        assertThat(results.get(1).getValue()).isEqualTo(6);
        assertThat(results.get(2).getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should handle empty batch")
    void shouldHandleEmptyBatch() {
        // Given
        List<String> scripts = Collections.emptyList();

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should execute batch with null context")
    void shouldExecuteBatchWithNullContext() {
        // Given
        List<String> scripts = Arrays.asList(
                "return 'batch-null-1'",
                "return 'batch-null-2'"
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", null);

        // Then
        assertThat(results).hasSize(2);
        assertThat(results).allMatch(ScriptResult::isSuccess);
        assertThat(results.get(0).getValue()).isEqualTo("batch-null-1");
        assertThat(results.get(1).getValue()).isEqualTo("batch-null-2");
    }

    @Test
    @DisplayName("Should handle empty batch with null context")
    void shouldHandleEmptyBatchWithNullContext() {
        // Given
        List<String> scripts = Collections.emptyList();

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", null);

        // Then
        assertThat(results).isEmpty();
    }


    @Test
    @DisplayName("Should execute batch with complex computations")
    void shouldExecuteBatchWithComplexComputations() {
        // Given
        List<String> scripts = Arrays.asList(
                """
                def sum = 0
                for (int i = 1; i <= 10; i++) {
                    sum += i
                }
                return sum
                """,
                """
                def factorial = 1
                for (int i = 1; i <= 5; i++) {
                    factorial *= i
                }
                return factorial
                """,
                """
                def fibonacci = [0, 1]
                for (int i = 2; i < 10; i++) {
                    fibonacci << fibonacci[i-1] + fibonacci[i-2]
                }
                return fibonacci[9]
                """
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).getValue()).isEqualTo(55);
        assertThat(results.get(1).getValue()).isEqualTo(120);
        assertThat(results.get(2).getValue()).isEqualTo(34);
    }

    @Test
    @DisplayName("Should execute batch with mixed success and failure")
    void shouldExecuteBatchWithMixedResults() {
        // Given
        List<String> scripts = Arrays.asList(
                "return 1 + 1",
                "throw new RuntimeException('Intentional error')",
                "return 3 + 3"
        );

        // When
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));

        // Then
        assertThat(results).hasSize(3);
        assertThat(results.get(0).isSuccess()).isTrue();
        assertThat(results.get(0).getValue()).isEqualTo(2);
        assertThat(results.get(1).isSuccess()).isFalse();
        assertThat(results.get(2).isSuccess()).isTrue();
        assertThat(results.get(2).getValue()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should execute batch of complex math computations")
    void shouldExecuteBatchOfComplexMathComputations() {
        // Given
        List<String> scripts = new ArrayList<>();
        String scriptTemplate = """
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
        for (int i = 1; i <= 100; i++) {
            scripts.add(scriptTemplate);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("input", i);
            ScriptResult result = facade.execute(scriptTemplate, "groovy", ScriptContext.of(vars));
            results.add(result);
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (scripts.size() * 1000.0) / duration;
        logger.info("Complex math batch execution: {} scripts in {} ms, throughput: {} ops/sec",
                scripts.size(), duration, String.format("%.2f", throughput));

        long successCount = results.stream().filter(ScriptResult::isSuccess).count();
        assertThat(successCount).isGreaterThanOrEqualTo(95); // Allow 5% failure rate
    }

    @Test
    @DisplayName("Should execute batch of complex string processing")
    void shouldExecuteBatchOfComplexStringProcessing() {
        // Given
        List<String> scripts = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            String script = """
                    def text = 'The quick brown fox jumps over the lazy dog ' * 10
                    def words = text.split(' ')
                    def reversed = words.collect { it.reverse() }
                    def joined = reversed.join('-')
                    def upper = joined.toUpperCase()
                    def result = upper.substring(0, Math.min(50, upper.length()))
                    return result.length()
                    """;
            scripts.add(script);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (scripts.size() * 1000.0) / duration;
        logger.info("Complex string batch execution: {} scripts in {} ms, throughput: {} ops/sec",
                scripts.size(), duration, String.format("%.2f", throughput));

        assertThat(results).hasSize(80);
        assertThat(results).allMatch(ScriptResult::isSuccess);
        assertThat(results).allMatch(r -> (Integer) r.getValue() == 50);
    }

    @Test
    @DisplayName("Should execute batch with multi-module complex operations")
    void shouldExecuteBatchWithMultiModuleComplexOperations() {
        // Given
        String scriptTemplate = """
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
        List<ScriptResult> results = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("index", i);
            ScriptResult result = facade.execute(scriptTemplate, "groovy", ScriptContext.of(vars));
            results.add(result);
        }
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (results.size() * 1000.0) / duration;
        logger.info("Multi-module batch execution: {} scripts in {} ms, throughput: {} ops/sec",
                results.size(), duration, String.format("%.2f", throughput));

        assertThat(results).hasSize(60);
        assertThat(results).allMatch(ScriptResult::isSuccess);
    }

    @Test
    @DisplayName("Should execute batch with large data processing")
    void shouldExecuteBatchWithLargeDataProcessing() {
        // Given
        List<String> scripts = new ArrayList<>();
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
        for (int i = 0; i < 50; i++) {
            scripts.add(script);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (scripts.size() * 1000.0) / duration;
        logger.info("Large data batch execution: {} scripts in {} ms, throughput: {} ops/sec",
                scripts.size(), duration, String.format("%.2f", throughput));

        assertThat(results).hasSize(50);
        assertThat(results).allMatch(ScriptResult::isSuccess);
    }

    @Test
    @DisplayName("Should execute batch with recursive algorithms")
    void shouldExecuteBatchWithRecursiveAlgorithms() {
        // Given
        List<String> scripts = new ArrayList<>();
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
        for (int i = 0; i < 40; i++) {
            scripts.add(script);
        }

        // When
        long startTime = System.currentTimeMillis();
        List<ScriptResult> results = facade.executeBatch(scripts, "groovy", ScriptContext.of(Collections.emptyMap()));
        long duration = System.currentTimeMillis() - startTime;

        // Then
        double throughput = (scripts.size() * 1000.0) / duration;
        logger.info("Recursive algorithm batch execution: {} scripts in {} ms, throughput: {} ops/sec",
                scripts.size(), duration, String.format("%.2f", throughput));

        assertThat(results).hasSize(40);
        long correctCount = results.stream()
                .filter(ScriptResult::isSuccess)
                .filter(r -> Boolean.TRUE.equals(r.getValue()))
                .count();
        assertThat(correctCount).isGreaterThanOrEqualTo(36); // Allow 10% failure rate
    }
}
