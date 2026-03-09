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

package io.github.koyan9.lingonexus.core.context;

/**
 * 脚本规范化器
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-01
 */
public class ScriptNormalizer {

    /**
     * 规范化脚本
     *
     * @param script 脚本内容
     * @return 规范化后的脚本
     */
    public static String normalize(String script) {
        if (script == null || script.trim().isEmpty()) {
            throw new IllegalArgumentException("Script cannot be null or empty");
        }

        String normalized = script.trim();

        normalized = removeComments(normalized);
        normalized = removeExtraWhitespace(normalized);
        normalized = normalizeLineEndings(normalized);

        return normalized;
    }

    private static String removeComments(String script) {
        StringBuilder result = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBlockComment = false;
        boolean inLineComment = false;

        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);

            if (c == '/' && i + 1 < script.length()) {
                char next = script.charAt(i + 1);
                if (next == '/') {
                    inBlockComment = !inBlockComment;
                    i++;
                } else if (next == '*') {
                    inBlockComment = false;
                }
            }

            if (c == '"') {
                if (inDoubleQuote) {
                    inDoubleQuote = false;
                } else {
                    inDoubleQuote = true;
                }
            } else if (c == '\'') {
                if (inSingleQuote) {
                    inSingleQuote = false;
                } else {
                    inSingleQuote = true;
                }
            }

            if (c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                inLineComment = true;
                i++;
            }

            if (c == '\n') {
                inLineComment = false;
            }

            if (!inBlockComment && !inLineComment) {
                result.append(c);
            }
        }

        return result.toString().trim();
    }

    private static String removeExtraWhitespace(String script) {
        return script.replaceAll("\\s+", " ");
    }

    private static String normalizeLineEndings(String script) {
        return script.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
    }
}
