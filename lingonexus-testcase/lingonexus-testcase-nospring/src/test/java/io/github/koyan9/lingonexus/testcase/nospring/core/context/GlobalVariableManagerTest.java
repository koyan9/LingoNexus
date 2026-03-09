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

package io.github.koyan9.lingonexus.testcase.nospring.core.context;

import io.github.koyan9.lingonexus.api.context.VariableManager;
import io.github.koyan9.lingonexus.core.context.GlobalVariableManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Global variable manager tests
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-03
 */
public class GlobalVariableManagerTest {

    private VariableManager manager;

    @BeforeEach
    void setUp() {
        manager = new GlobalVariableManager();
    }

    @Test
    @DisplayName("Should set and get variable")
    void shouldSetAndGetVariable() {
        manager.setVariable("key1", "value1");
        assertEquals("value1", manager.getVariable("key1"));
    }

    @Test
    @DisplayName("Should check if variable exists")
    void shouldCheckIfVariableExists() {
        assertFalse(manager.hasVariable("key1"));
        manager.setVariable("key1", "value1");
        assertTrue(manager.hasVariable("key1"));
    }

    @Test
    @DisplayName("Should remove variable")
    void shouldRemoveVariable() {
        manager.setVariable("key1", "value1");
        assertTrue(manager.hasVariable("key1"));

        manager.removeVariable("key1");
        assertFalse(manager.hasVariable("key1"));
        assertNull(manager.getVariable("key1"));
    }

    @Test
    @DisplayName("Should clear all variables")
    void shouldClearAllVariables() {
        manager.setVariable("key1", "value1");
        manager.setVariable("key2", "value2");
        manager.setVariable("key3", "value3");

        manager.clear();

        assertFalse(manager.hasVariable("key1"));
        assertFalse(manager.hasVariable("key2"));
        assertFalse(manager.hasVariable("key3"));
    }

    @Test
    @DisplayName("Should get all variables")
    void shouldGetAllVariables() {
        manager.setVariable("key1", "value1");
        manager.setVariable("key2", 42);
        manager.setVariable("key3", true);

        Map<String, Object> allVars = manager.getAllVariables();

        assertEquals(3, allVars.size());
        assertEquals("value1", allVars.get("key1"));
        assertEquals(42, allVars.get("key2"));
        assertEquals(true, allVars.get("key3"));
    }

    @Test
    @DisplayName("Should increment version on changes")
    void shouldIncrementVersionOnChanges() {
        long version1 = manager.getVersion();

        manager.setVariable("key1", "value1");
        long version2 = manager.getVersion();
        assertTrue(version2 > version1);

        manager.setVariable("key2", "value2");
        long version3 = manager.getVersion();
        assertTrue(version3 > version2);

        manager.removeVariable("key1");
        long version4 = manager.getVersion();
        assertTrue(version4 > version3);

        manager.clear();
        long version5 = manager.getVersion();
        assertTrue(version5 > version4);
    }

    @Test
    @DisplayName("Should handle different value types")
    void shouldHandleDifferentValueTypes() {
        manager.setVariable("string", "text");
        manager.setVariable("integer", 123);
        manager.setVariable("double", 45.67);
        manager.setVariable("boolean", true);
        manager.setVariable("object", new Object());

        assertEquals("text", manager.getVariable("string"));
        assertEquals(123, manager.getVariable("integer"));
        assertEquals(45.67, manager.getVariable("double"));
        assertEquals(true, manager.getVariable("boolean"));
        assertNotNull(manager.getVariable("object"));
    }
}
