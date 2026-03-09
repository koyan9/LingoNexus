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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FormatterModuleTest {

    private FormatterModule formatter;

    @BeforeEach
    void setUp() {
        formatter = new FormatterModule();
    }

    @Test
    void testModuleName() {
        assertEquals("formatter", formatter.getName());
    }

    @Test
    void testNumber() {
        assertEquals("1,234,567.89", formatter.number(1234567.89, "#,##0.00"));
        assertEquals("", formatter.number(null, "#,##0"));
    }

    @Test
    void testCurrency() {
        assertEquals("1,234.50", formatter.currency(1234.5));
        assertEquals("0.00", formatter.currency(0));
        assertEquals("", formatter.currency(null));
    }

    @Test
    void testPercent() {
        assertEquals("85.60%", formatter.percent(0.856));
        assertEquals("100.00%", formatter.percent(1.0));
        assertEquals("0.00%", formatter.percent(0));
        assertEquals("", formatter.percent(null));
    }

    @Test
    void testPadLeft() {
        assertEquals("00042", formatter.padLeft("42", 5, '0'));
        assertEquals("42", formatter.padLeft("42", 2, '0'));
        assertEquals("42", formatter.padLeft("42", 1, '0'));
        assertEquals("000", formatter.padLeft("", 3, '0'));
    }

    @Test
    void testPadRight() {
        assertEquals("hi...", formatter.padRight("hi", 5, '.'));
        assertEquals("hi", formatter.padRight("hi", 2, '.'));
    }

    @Test
    void testMask() {
        assertEquals("138****8000", formatter.mask("13800138000", 3, 7));
        assertEquals("****", formatter.mask("test", 0, 4));
        assertNull(formatter.mask(null, 0, 1));
        assertEquals("", formatter.mask("", 0, 1));
    }

    @Test
    void testCapitalize() {
        assertEquals("Hello", formatter.capitalize("hello"));
        assertNull(formatter.capitalize(null));
        assertEquals("", formatter.capitalize(""));
    }

    @Test
    void testCamelCase() {
        assertEquals("helloWorld", formatter.camelCase("hello_world"));
        assertEquals("helloWorld", formatter.camelCase("hello-world"));
        assertEquals("helloWorld", formatter.camelCase("hello world"));
        assertNull(formatter.camelCase(null));
    }

    @Test
    void testSnakeCase() {
        assertEquals("hello_world", formatter.snakeCase("helloWorld"));
        assertEquals("hello_world", formatter.snakeCase("hello-world"));
        assertEquals("hello_world", formatter.snakeCase("hello world"));
        assertNull(formatter.snakeCase(null));
    }

    @Test
    void testHasFunction() {
        assertTrue(formatter.hasFunction("number"));
        assertTrue(formatter.hasFunction("currency"));
        assertTrue(formatter.hasFunction("percent"));
        assertTrue(formatter.hasFunction("mask"));
        assertFalse(formatter.hasFunction("unknown"));
    }
}
