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

import io.github.koyan9.lingonexus.api.constant.LingoNexusConstants;
import io.github.koyan9.lingonexus.api.module.spi.ScriptModule;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 编码/加密工具模块 - 提供常用编码与摘要功能
 *
 * <p>脚本中使用示例:</p>
 * <pre>{@code
 * codec.base64Encode("hello")            // "aGVsbG8="
 * codec.base64Decode("aGVsbG8=")          // "hello"
 * codec.md5("hello")                      // "5d41402abc4b2a76b9719d911017c592"
 * codec.sha256("hello")                   // "2cf24dba..."
 * codec.uuid()                            // "550e8400-e29b-..."
 * }</pre>
 *
 * <p>该模块是无状态的，可以安全地在多个实例之间共享。</p>
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-02
 */
public class CodecModule implements ScriptModule {

    private static final String MODULE_NAME = LingoNexusConstants.MODULE_CODEC;
    private final Map<String, Object> functions;

    public CodecModule() {
        this.functions = new HashMap<>();
        initFunctions();
    }

    private void initFunctions() {
        functions.put("base64Encode", (Function<String, String>) this::base64Encode);
        functions.put("base64Decode", (Function<String, String>) this::base64Decode);
        functions.put("md5", (Function<String, String>) this::md5);
        functions.put("sha256", (Function<String, String>) this::sha256);
        functions.put("sha512", (Function<String, String>) this::sha512);
        functions.put("uuid", (Supplier<String>) this::uuid);
        functions.put("hexEncode", (Function<byte[], String>) this::hexEncode);
        functions.put("urlEncode", (Function<String, String>) this::urlEncode);
        functions.put("urlDecode", (Function<String, String>) this::urlDecode);
    }

    @Override
    public String getName() {
        return MODULE_NAME;
    }

    @Override
    public Map<String, Object> getFunctions() {
        return Collections.unmodifiableMap(functions);
    }

    @Override
    public boolean hasFunction(String functionName) {
        return functions.containsKey(functionName);
    }

    @Override
    public String getDescription() {
        return "Encoding and hashing utility module (Base64, MD5, SHA, URL encoding)";
    }

    public String base64Encode(String value) {
        if (value == null) return null;
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    public String base64Decode(String value) {
        if (value == null) return null;
        return new String(Base64.getDecoder().decode(value));
    }

    public String md5(String value) {
        return digest(value, "MD5");
    }

    public String sha256(String value) {
        return digest(value, "SHA-256");
    }

    public String sha512(String value) {
        return digest(value, "SHA-512");
    }

    public String uuid() {
        return UUID.randomUUID().toString();
    }

    public String hexEncode(byte[] bytes) {
        if (bytes == null) return null;
        return bytesToHex(bytes);
    }

    public String urlEncode(String value) {
        if (value == null) return null;
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String urlDecode(String value) {
        if (value == null) return null;
        return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String digest(String value, String algorithm) {
        if (value == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] hash = md.digest(value.getBytes());
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm not available: " + algorithm, e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
