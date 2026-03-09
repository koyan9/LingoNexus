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

import io.github.koyan9.lingonexus.modules.ConvertModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConvertModuleTest {

    private ConvertModule convert;

    @BeforeEach
    void setUp() {
        convert = new ConvertModule();
    }

    @Test
    void testModuleName() {
        assertEquals("convert", convert.getName());
    }

    @Test
    void testToString() {
        assertEquals("123", convert.toString(123));
        assertEquals("true", convert.toString(true));
        assertNull(convert.toString(null));
    }

    @Test
    void testToInteger() {
        assertEquals(42, convert.toInteger("42"));
        assertEquals(42, convert.toInteger(42));
        assertEquals(42, convert.toInteger(42L));
        assertEquals(42, convert.toInteger(42.9));
        assertNull(convert.toInteger(null));
        assertThrows(NumberFormatException.class, () -> convert.toInteger("abc"));
    }

    @Test
    void testToLong() {
        assertEquals(1000000L, convert.toLong("1000000"));
        assertEquals(42L, convert.toLong(42));
        assertEquals(42L, convert.toLong(42L));
        assertNull(convert.toLong(null));
    }

    @Test
    void testToDouble() {
        assertEquals(3.14, convert.toDouble("3.14"), 0.0001);
        assertEquals(42.0, convert.toDouble(42));
        assertEquals(42.0, convert.toDouble(42.0));
        assertNull(convert.toDouble(null));
    }

    @Test
    void testToBoolean() {
        assertEquals(true, convert.toBoolean("true"));
        assertEquals(true, convert.toBoolean("TRUE"));
        assertEquals(true, convert.toBoolean("1"));
        assertEquals(true, convert.toBoolean("yes"));
        assertEquals(true, convert.toBoolean(true));
        assertEquals(false, convert.toBoolean("false"));
        assertEquals(false, convert.toBoolean("0"));
        assertEquals(false, convert.toBoolean("no"));
        assertNull(convert.toBoolean(null));
    }

    @Test
    void testHasFunction() {
        assertTrue(convert.hasFunction("toString"));
        assertTrue(convert.hasFunction("toInteger"));
        assertTrue(convert.hasFunction("toLong"));
        assertTrue(convert.hasFunction("toDouble"));
        assertTrue(convert.hasFunction("toBoolean"));
        assertFalse(convert.hasFunction("unknown"));
    }
}
