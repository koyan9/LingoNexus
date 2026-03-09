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

package io.github.koyan9.lingonexus.testcase.nospring.api.lang;

import io.github.koyan9.lingonexus.api.lang.ScriptLanguage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptLanguageTest {

    @Test
    void testGroovyLanguage() {
        Assertions.assertEquals("groovy", ScriptLanguage.GROOVY.getId());
        assertEquals("Groovy", ScriptLanguage.GROOVY.getDisplayName());
        assertTrue(ScriptLanguage.GROOVY.matches("groovy"));
        assertTrue(ScriptLanguage.GROOVY.matches("GROOVY"));
        assertTrue(ScriptLanguage.GROOVY.matches(" groovy "));
        assertFalse(ScriptLanguage.GROOVY.matches("javascript"));
        assertFalse(ScriptLanguage.GROOVY.matches(null));
        assertFalse(ScriptLanguage.GROOVY.matches(""));
    }

    @Test
    void testJavaScriptLanguage() {
        assertEquals("javascript", ScriptLanguage.JAVASCRIPT.getId());
        assertTrue(ScriptLanguage.JAVASCRIPT.matches("javascript"));
        assertTrue(ScriptLanguage.JAVASCRIPT.matches("js"));
        assertTrue(ScriptLanguage.JAVASCRIPT.matches("ecmascript"));
        assertFalse(ScriptLanguage.JAVASCRIPT.matches("java"));
    }

    @Test
    void testJavaLanguage() {
        assertEquals("javaexpr", ScriptLanguage.JAVAEXPR.getId());
        assertTrue(ScriptLanguage.JAVAEXPR.matches("javaexpr"));
        assertTrue(ScriptLanguage.JAVAEXPR.matches("jexpr"));
        assertFalse(ScriptLanguage.JAVAEXPR.matches("javascript"));
    }

    @Test
    void testKotlinLanguage() {
        assertEquals("kotlin", ScriptLanguage.KOTLIN.getId());
        assertTrue(ScriptLanguage.KOTLIN.matches("kotlin"));
        assertTrue(ScriptLanguage.KOTLIN.matches("kts"));
        assertFalse(ScriptLanguage.KOTLIN.matches("java"));
    }

    @Test
    void testFromString() {
        assertEquals(ScriptLanguage.GROOVY, ScriptLanguage.fromString("groovy"));
        assertEquals(ScriptLanguage.JAVASCRIPT, ScriptLanguage.fromString("js"));
        assertEquals(ScriptLanguage.JAVASCRIPT, ScriptLanguage.fromString("javascript"));
        assertEquals(ScriptLanguage.JAVASCRIPT, ScriptLanguage.fromString("ecmascript"));
        assertEquals(ScriptLanguage.JAVAEXPR, ScriptLanguage.fromString("javaexpr"));
        assertEquals(ScriptLanguage.JAVAEXPR, ScriptLanguage.fromString("jexpr"));
        assertEquals(ScriptLanguage.KOTLIN, ScriptLanguage.fromString("kotlin"));
        assertEquals(ScriptLanguage.KOTLIN, ScriptLanguage.fromString("kts"));
        assertNull(ScriptLanguage.fromString("unknown"));
        assertNull(ScriptLanguage.fromString(null));
        assertNull(ScriptLanguage.fromString(""));
    }

    @Test
    void testFromStringOrThrow() {
        assertEquals(ScriptLanguage.GROOVY, ScriptLanguage.fromStringOrThrow("groovy"));
        assertThrows(IllegalArgumentException.class, () -> ScriptLanguage.fromStringOrThrow("unknown"));
    }

    @Test
    void testIsSupported() {
        assertTrue(ScriptLanguage.isSupported("groovy"));
        assertTrue(ScriptLanguage.isSupported("javascript"));
        assertTrue(ScriptLanguage.isSupported("js"));
        assertTrue(ScriptLanguage.isSupported("java"));
        assertTrue(ScriptLanguage.isSupported("kotlin"));
        assertFalse(ScriptLanguage.isSupported("python"));
        assertFalse(ScriptLanguage.isSupported(null));
    }

    @Test
    void testGetDefault() {
        assertEquals(ScriptLanguage.GROOVY, ScriptLanguage.getDefault());
    }

    @Test
    void testToString() {
        assertEquals("groovy", ScriptLanguage.GROOVY.toString());
        assertEquals("javascript", ScriptLanguage.JAVASCRIPT.toString());
        assertEquals("javaexpr", ScriptLanguage.JAVAEXPR.toString());
        assertEquals("kotlin", ScriptLanguage.KOTLIN.toString());
    }

    @Test
    void testAliasesAreUnmodifiable() {
        assertThrows(UnsupportedOperationException.class, () ->
                ScriptLanguage.GROOVY.getAliases().add("test"));
    }
}
