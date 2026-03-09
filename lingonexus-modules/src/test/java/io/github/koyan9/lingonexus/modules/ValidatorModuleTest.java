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

class ValidatorModuleTest {

    private ValidatorModule validator;

    @BeforeEach
    void setUp() {
        validator = new ValidatorModule();
    }

    @Test
    void testModuleName() {
        assertEquals("validator", validator.getName());
    }

    @Test
    void testIsEmail() {
        assertTrue(validator.isEmail("test@example.com"));
        assertTrue(validator.isEmail("user.name+tag@domain.co"));
        assertFalse(validator.isEmail("invalid"));
        assertFalse(validator.isEmail("@domain.com"));
        assertFalse(validator.isEmail(null));
    }

    @Test
    void testIsPhone() {
        assertTrue(validator.isPhone("13800138000"));
        assertTrue(validator.isPhone("15912345678"));
        assertFalse(validator.isPhone("12345678901"));
        assertFalse(validator.isPhone("1380013800"));
        assertFalse(validator.isPhone(null));
    }

    @Test
    void testIsUrl() {
        assertTrue(validator.isUrl("https://example.com"));
        assertTrue(validator.isUrl("http://www.example.com/path"));
        assertFalse(validator.isUrl("not-a-url"));
        assertFalse(validator.isUrl("ftp://example.com"));
        assertFalse(validator.isUrl(null));
    }

    @Test
    void testIsIpv4() {
        assertTrue(validator.isIpv4("192.168.1.1"));
        assertTrue(validator.isIpv4("0.0.0.0"));
        assertTrue(validator.isIpv4("255.255.255.255"));
        assertFalse(validator.isIpv4("256.1.1.1"));
        assertFalse(validator.isIpv4("1.2.3"));
        assertFalse(validator.isIpv4(null));
    }

    @Test
    void testIsIdCard() {
        assertTrue(validator.isIdCard("110101199003076530"));
        assertTrue(validator.isIdCard("11010119900307653X"));
        assertFalse(validator.isIdCard("12345"));
        assertFalse(validator.isIdCard(null));
    }

    @Test
    void testIsNumber() {
        assertTrue(validator.isNumber("123"));
        assertTrue(validator.isNumber("3.14"));
        assertTrue(validator.isNumber("-10"));
        assertFalse(validator.isNumber("abc"));
        assertFalse(validator.isNumber(null));
        assertFalse(validator.isNumber(""));
    }

    @Test
    void testRange() {
        assertTrue(validator.range(5, 1, 10));
        assertTrue(validator.range(1, 1, 10));
        assertTrue(validator.range(10, 1, 10));
        assertFalse(validator.range(0, 1, 10));
        assertFalse(validator.range(11, 1, 10));
        assertFalse(validator.range(null, 1, 10));
    }

    @Test
    void testLengthRange() {
        assertTrue(validator.lengthRange("hello", 3, 10));
        assertTrue(validator.lengthRange("hi", 2, 2));
        assertFalse(validator.lengthRange("hi", 3, 10));
        assertFalse(validator.lengthRange(null, 0, 10));
    }

    @Test
    void testNotNull() {
        assertTrue(validator.notNull("test"));
        assertTrue(validator.notNull(0));
        assertFalse(validator.notNull(null));
    }

    @Test
    void testMatches() {
        assertTrue(validator.matches("abc123", "^[a-z]+\\d+$"));
        assertFalse(validator.matches("ABC", "^[a-z]+$"));
        assertFalse(validator.matches(null, ".*"));
        assertFalse(validator.matches("test", null));
    }

    @Test
    void testHasFunction() {
        assertTrue(validator.hasFunction("isEmail"));
        assertTrue(validator.hasFunction("isPhone"));
        assertTrue(validator.hasFunction("range"));
        assertFalse(validator.hasFunction("unknown"));
    }
}
