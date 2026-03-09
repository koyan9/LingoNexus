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

import io.github.koyan9.lingonexus.modules.CollectionModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CollectionModuleTest {

    private CollectionModule col;

    @BeforeEach
    void setUp() {
        col = new CollectionModule();
    }

    @Test
    void testModuleName() {
        assertEquals("col", col.getName());
    }

    @Test
    void testSize() {
        assertEquals(3, col.size(Arrays.asList(1, 2, 3)));
        assertEquals(0, col.size(null));
        assertEquals(0, col.size(Collections.emptyList()));
    }

    @Test
    void testIsEmpty() {
        assertTrue(col.isEmpty(null));
        assertTrue(col.isEmpty(Collections.emptyList()));
        assertFalse(col.isEmpty(Arrays.asList(1)));
    }

    @Test
    void testIsNotEmpty() {
        assertFalse(col.isNotEmpty(null));
        assertTrue(col.isNotEmpty(Arrays.asList(1)));
    }

    @Test
    void testContains() {
        assertTrue(col.contains(Arrays.asList(1, 2, 3), 2));
        assertFalse(col.contains(Arrays.asList(1, 2, 3), 4));
        assertFalse(col.contains(null, 1));
    }

    @Test
    void testToList() {
        List<?> result = col.toList(Arrays.asList(1, 2, 3));
        assertEquals(3, result.size());
        assertInstanceOf(ArrayList.class, result);
        assertTrue(col.toList(null).isEmpty());
    }

    @Test
    void testToMap() {
        assertNotNull(col.toMap(Collections.singletonMap("key", "val")));
        assertTrue(col.toMap(null).isEmpty());
    }

    @Test
    void testFirst() {
        assertEquals(1, col.first(Arrays.asList(1, 2, 3)));
        assertNull(col.first(null));
        assertNull(col.first(Collections.emptyList()));
    }

    @Test
    void testLast() {
        assertEquals(3, col.last(Arrays.asList(1, 2, 3)));
        assertNull(col.last(null));
        assertNull(col.last(Collections.emptyList()));
    }

    @Test
    void testSubList() {
        List<?> result = col.subList(Arrays.asList(1, 2, 3, 4, 5), 1, 3);
        assertEquals(Arrays.asList(2, 3), result);
        assertTrue(col.subList(null, 0, 1).isEmpty());
        assertTrue(col.subList(Arrays.asList(1, 2), 5, 2).isEmpty());
    }

    @Test
    void testFlatten() {
        List<List<Integer>> nested = Arrays.asList(
                Arrays.asList(1, 2),
                Arrays.asList(3, 4)
        );
        List<?> result = col.flatten(nested);
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
        assertTrue(col.flatten(null).isEmpty());
    }

    @Test
    void testHasFunction() {
        assertTrue(col.hasFunction("size"));
        assertTrue(col.hasFunction("isEmpty"));
        assertTrue(col.hasFunction("first"));
        assertTrue(col.hasFunction("last"));
        assertFalse(col.hasFunction("unknown"));
    }
}
