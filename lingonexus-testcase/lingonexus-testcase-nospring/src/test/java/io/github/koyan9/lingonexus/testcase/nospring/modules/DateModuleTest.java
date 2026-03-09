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

import io.github.koyan9.lingonexus.modules.DateModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DateModuleTest {

    private DateModule date;

    @BeforeEach
    void setUp() {
        date = new DateModule();
    }

    @Test
    void testModuleName() {
        assertEquals("date", date.getName());
    }

    @Test
    void testNow() {
        String result = date.now("yyyy-MM-dd");
        assertNotNull(result);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void testTimestamp() {
        long ts = date.timestamp();
        assertTrue(ts > 0);
    }

    @Test
    void testFormat() {
        // Use a known timestamp
        String result = date.format(0L, "yyyy");
        assertNotNull(result);
    }

    @Test
    void testParseAndFormat() {
        long ts = date.parse("2026-01-15", "yyyy-MM-dd");
        String formatted = date.format(ts, "yyyy-MM-dd");
        assertEquals("2026-01-15", formatted);
    }

    @Test
    void testAddDays() {
        long ts = date.parse("2026-01-01", "yyyy-MM-dd");
        long result = date.addDays(ts, 10);
        String formatted = date.format(result, "yyyy-MM-dd");
        assertEquals("2026-01-11", formatted);
    }

    @Test
    void testAddHours() {
        long ts = date.parse("2026-01-01 00:00:00", "yyyy-MM-dd HH:mm:ss");
        long result = date.addHours(ts, 25);
        String formatted = date.format(result, "yyyy-MM-dd HH:mm:ss");
        assertEquals("2026-01-02 01:00:00", formatted);
    }

    @Test
    void testDaysBetween() {
        long ts1 = date.parse("2026-01-01", "yyyy-MM-dd");
        long ts2 = date.parse("2026-01-11", "yyyy-MM-dd");
        assertEquals(10, date.daysBetween(ts1, ts2));
        assertEquals(10, date.daysBetween(ts2, ts1)); // Should be absolute
    }

    @Test
    void testHasFunction() {
        assertTrue(date.hasFunction("now"));
        assertTrue(date.hasFunction("timestamp"));
        assertTrue(date.hasFunction("format"));
        assertTrue(date.hasFunction("parse"));
        assertTrue(date.hasFunction("addDays"));
        assertTrue(date.hasFunction("addHours"));
        assertTrue(date.hasFunction("daysBetween"));
        assertFalse(date.hasFunction("unknown"));
    }
}
