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

package io.github.koyan9.lingonexus.testcase.nospring.modules;

import io.github.koyan9.lingonexus.modules.MathModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MathModuleTest {

    private MathModule math;

    @BeforeEach
    void setUp() {
        math = new MathModule();
    }

    @Test
    void testModuleName() {
        assertEquals("math", math.getName());
    }

    @Test
    void testHasFunction() {
        assertTrue(math.hasFunction("add"));
        assertTrue(math.hasFunction("subtract"));
        assertTrue(math.hasFunction("multiply"));
        assertTrue(math.hasFunction("divide"));
        assertTrue(math.hasFunction("round"));
        assertTrue(math.hasFunction("random"));
        assertTrue(math.hasFunction("max"));
        assertTrue(math.hasFunction("min"));
        assertTrue(math.hasFunction("abs"));
        assertTrue(math.hasFunction("pow"));
        assertTrue(math.hasFunction("sqrt"));
        assertTrue(math.hasFunction("floor"));
        assertTrue(math.hasFunction("ceil"));
        assertTrue(math.hasFunction("scale"));
        assertFalse(math.hasFunction("nonexistent"));
    }

    @Test
    void testAdd() {
        assertEquals(3.3, math.add(1.1, 2.2), 0.0001);
        assertEquals(0.3, math.add(0.1, 0.2), 0.0001);
        assertEquals(0.0, math.add(1.0, -1.0), 0.0001);
    }

    @Test
    void testSubtract() {
        assertEquals(1.0, math.subtract(3.0, 2.0), 0.0001);
        assertEquals(-1.0, math.subtract(2.0, 3.0), 0.0001);
    }

    @Test
    void testMultiply() {
        assertEquals(12.0, math.multiply(3.0, 4.0), 0.0001);
        assertEquals(0.0, math.multiply(0.0, 100.0), 0.0001);
    }

    @Test
    void testDivide() {
        assertEquals(2.5, math.divide(5.0, 2.0), 0.0001);
        assertThrows(ArithmeticException.class, () -> math.divide(1.0, 0.0));
    }

    @Test
    void testRound() {
        assertEquals(4, math.round(3.7));
        assertEquals(3, math.round(3.3));
        assertEquals(4, math.round(3.5));
    }

    @Test
    void testRandom() {
        double val = math.random();
        assertTrue(val >= 0.0 && val < 1.0);
    }

    @Test
    void testMaxMin() {
        assertEquals(20.0, math.max(10.0, 20.0));
        assertEquals(10.0, math.min(10.0, 20.0));
    }

    @Test
    void testAbs() {
        assertEquals(5.0, math.abs(-5.0));
        assertEquals(5.0, math.abs(5.0));
    }

    @Test
    void testPow() {
        assertEquals(1024.0, math.pow(2, 10));
        assertEquals(1.0, math.pow(5, 0));
    }

    @Test
    void testSqrt() {
        assertEquals(3.0, math.sqrt(9.0), 0.0001);
        assertEquals(0.0, math.sqrt(0.0));
    }

    @Test
    void testFloorCeil() {
        assertEquals(3.0, math.floor(3.7));
        assertEquals(4.0, math.ceil(3.1));
    }

    @Test
    void testScale() {
        assertEquals(3.14, math.scale(3.14159, 2, "HALF_UP"), 0.0001);
        assertEquals(3.15, math.scale(3.145, 2, "UP"), 0.0001);
        assertEquals(3.14, math.scale(3.145, 2, "DOWN"), 0.0001);
        assertEquals(3.14, math.scale(3.145, 2, "HALF_DOWN"), 0.0001);
    }

    @Test
    void testGetDescription() {
        assertNotNull(math.getDescription());
    }
}
