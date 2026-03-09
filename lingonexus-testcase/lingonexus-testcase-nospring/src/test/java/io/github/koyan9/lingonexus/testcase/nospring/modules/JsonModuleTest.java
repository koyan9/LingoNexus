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

import io.github.koyan9.lingonexus.modules.JsonModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonModuleTest {

    private JsonModule json;

    @BeforeEach
    void setUp() {
        json = new JsonModule();
    }

    @Test
    void testModuleName() {
        assertEquals("json", json.getName());
    }

    @Test
    void testToJson() {
        assertEquals("\"hello\"", json.toJson("hello"));
        assertEquals("123", json.toJson(123));
        assertEquals("null", json.toJson(null));
        String mapJson = json.toJson(Map.of("name", "test"));
        assertTrue(mapJson.contains("\"name\""));
        assertTrue(mapJson.contains("\"test\""));
    }

    @Test
    void testFromJson() {
        assertNull(json.fromJson(null));
        assertNull(json.fromJson(""));
        assertEquals("hello", json.fromJson("\"hello\""));
        assertEquals(123, json.fromJson("123"));

        Object result = json.fromJson("{\"name\":\"test\"}");
        assertInstanceOf(Map.class, result);
    }

    @Test
    void testParsePath() {
        String jsonStr = "{\"user\":{\"name\":\"Alice\",\"age\":25}}";
        assertEquals("Alice", json.parsePath(jsonStr, "user.name"));
        assertEquals(25, json.parsePath(jsonStr, "user.age"));
        assertNull(json.parsePath(jsonStr, "user.email"));
        assertNull(json.parsePath(null, "test"));
        assertNull(json.parsePath(jsonStr, null));
    }

    @Test
    void testParsePathWithArray() {
        String jsonStr = "{\"items\":[\"a\",\"b\",\"c\"]}";
        assertEquals("a", json.parsePath(jsonStr, "items[0]"));
        assertEquals("c", json.parsePath(jsonStr, "items[2]"));
    }

    @Test
    void testParsePathBoolean() {
        String jsonStr = "{\"active\":true}";
        assertEquals(true, json.parsePath(jsonStr, "active"));
    }

    @Test
    void testParsePathNull() {
        String jsonStr = "{\"value\":null}";
        assertNull(json.parsePath(jsonStr, "value"));
    }

    @Test
    void testInvalidJson() {
        assertThrows(RuntimeException.class, () -> json.fromJson("invalid"));
        assertThrows(RuntimeException.class, () -> json.toJson(new Object() {
            // Unserializable (self-referencing would cause this, but let's keep it simple)
        }));
    }

    @Test
    void testHasFunction() {
        assertTrue(json.hasFunction("toJson"));
        assertTrue(json.hasFunction("fromJson"));
        assertTrue(json.hasFunction("parsePath"));
        assertFalse(json.hasFunction("unknown"));
    }
}
