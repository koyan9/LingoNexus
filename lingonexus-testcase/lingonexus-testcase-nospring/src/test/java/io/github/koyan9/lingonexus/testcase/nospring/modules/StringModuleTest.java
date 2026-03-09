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

import io.github.koyan9.lingonexus.modules.StringModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StringModuleTest {

    private StringModule str;

    @BeforeEach
    void setUp() {
        str = new StringModule();
    }

    @Test
    void testModuleName() {
        assertEquals("str", str.getName());
    }

    @Test
    void testIsBlank() {
        assertTrue(str.isBlank(null));
        assertTrue(str.isBlank(""));
        assertTrue(str.isBlank("   "));
        assertFalse(str.isBlank("hello"));
    }

    @Test
    void testIsNotBlank() {
        assertFalse(str.isNotBlank(null));
        assertFalse(str.isNotBlank(""));
        assertTrue(str.isNotBlank("hello"));
    }

    @Test
    void testTrim() {
        assertEquals("hello", str.trim("  hello  "));
        assertNull(str.trim(null));
    }

    @Test
    void testLength() {
        assertEquals(5, str.length("hello"));
        assertEquals(0, str.length(null));
        assertEquals(0, str.length(""));
    }

    @Test
    void testSubstring() {
        assertEquals("ell", str.substring("hello", 1, 4));
        assertNull(str.substring(null, 0, 1));
        assertEquals("", str.substring("hello", 5, 2));
        assertEquals("hello", str.substring("hello", -1, 100));
    }

    @Test
    void testMatches() {
        assertTrue(str.matches("test@example.com", ".*@.*\\..*"));
        assertFalse(str.matches(null, ".*"));
        assertFalse(str.matches("test", null));
    }

    @Test
    void testSplit() {
        List<String> result = str.split("a,b,c", ",");
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertTrue(str.split(null, ",").isEmpty());
    }

    @Test
    void testJoin() {
        assertEquals("a-b-c", str.join(Arrays.asList("a", "b", "c"), "-"));
        assertEquals("", str.join(null, "-"));
        assertEquals("", str.join(Arrays.asList(), "-"));
    }

    @Test
    void testContains() {
        assertTrue(str.contains("hello world", "world"));
        assertFalse(str.contains("hello", "world"));
        assertFalse(str.contains(null, "test"));
    }

    @Test
    void testStartsEndsWith() {
        assertTrue(str.startsWith("hello", "hel"));
        assertFalse(str.startsWith("hello", "world"));
        assertFalse(str.startsWith(null, "test"));
        assertTrue(str.endsWith("hello", "llo"));
        assertFalse(str.endsWith(null, "test"));
    }

    @Test
    void testReplace() {
        assertEquals("hello world", str.replace("hello java", "java", "world"));
        assertNull(str.replace(null, "a", "b"));
    }

    @Test
    void testCase() {
        assertEquals("HELLO", str.toUpperCase("hello"));
        assertEquals("hello", str.toLowerCase("HELLO"));
        assertNull(str.toUpperCase(null));
        assertNull(str.toLowerCase(null));
    }

    @Test
    void testHasFunction() {
        assertTrue(str.hasFunction("isBlank"));
        assertTrue(str.hasFunction("split"));
        assertTrue(str.hasFunction("join"));
        assertFalse(str.hasFunction("unknown"));
    }
}
