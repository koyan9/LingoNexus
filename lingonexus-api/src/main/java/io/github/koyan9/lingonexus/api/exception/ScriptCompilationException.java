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
package io.github.koyan9.lingonexus.api.exception;

/**
 * 脚本编译异常
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class ScriptCompilationException extends ScriptException {

    private final int lineNumber;
    private final int columnNumber;

    public ScriptCompilationException(String message) {
        this(message, -1, -1);
    }

    public ScriptCompilationException(String message, int lineNumber, int columnNumber) {
        super(message);
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    public ScriptCompilationException(String message, Throwable cause) {
        super(message, cause);
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public int getColumnNumber() {
        return columnNumber;
    }
}
