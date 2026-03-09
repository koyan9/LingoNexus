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

class CodecModuleTest {

    private CodecModule codec;

    @BeforeEach
    void setUp() {
        codec = new CodecModule();
    }

    @Test
    void testModuleName() {
        assertEquals("codec", codec.getName());
    }

    @Test
    void testBase64Encode() {
        assertEquals("SGVsbG8=", codec.base64Encode("Hello"));
        assertEquals("", codec.base64Encode(""));
        assertNull(codec.base64Encode(null));
    }

    @Test
    void testBase64Decode() {
        assertEquals("Hello", codec.base64Decode("SGVsbG8="));
        assertNull(codec.base64Decode(null));
    }

    @Test
    void testBase64RoundTrip() {
        String original = "LingoNexus 测试数据 123!@#";
        String encoded = codec.base64Encode(original);
        String decoded = codec.base64Decode(encoded);
        assertEquals(original, decoded);
    }

    @Test
    void testMd5() {
        assertEquals("5d41402abc4b2a76b9719d911017c592", codec.md5("hello"));
        assertNull(codec.md5(null));
    }

    @Test
    void testSha256() {
        String hash = codec.sha256("hello");
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 produces 64 hex chars
        assertNull(codec.sha256(null));
    }

    @Test
    void testSha512() {
        String hash = codec.sha512("hello");
        assertNotNull(hash);
        assertEquals(128, hash.length()); // SHA-512 produces 128 hex chars
        assertNull(codec.sha512(null));
    }

    @Test
    void testUuid() {
        String uuid = codec.uuid();
        assertNotNull(uuid);
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
        // Should be unique
        assertNotEquals(codec.uuid(), codec.uuid());
    }

    @Test
    void testHexEncode() {
        assertEquals("48656c6c6f", codec.hexEncode("Hello".getBytes()));
        assertNull(codec.hexEncode(null));
    }

    @Test
    void testUrlEncode() {
        assertEquals("hello+world", codec.urlEncode("hello world"));
        assertEquals("a%3Db", codec.urlEncode("a=b"));
        assertNull(codec.urlEncode(null));
    }

    @Test
    void testUrlDecode() {
        assertEquals("hello world", codec.urlDecode("hello+world"));
        assertEquals("a=b", codec.urlDecode("a%3Db"));
        assertNull(codec.urlDecode(null));
    }

    @Test
    void testUrlRoundTrip() {
        String original = "key=value&foo=bar baz";
        assertEquals(original, codec.urlDecode(codec.urlEncode(original)));
    }

    @Test
    void testHasFunction() {
        assertTrue(codec.hasFunction("base64Encode"));
        assertTrue(codec.hasFunction("base64Decode"));
        assertTrue(codec.hasFunction("md5"));
        assertTrue(codec.hasFunction("sha256"));
        assertTrue(codec.hasFunction("uuid"));
        assertFalse(codec.hasFunction("unknown"));
    }
}
