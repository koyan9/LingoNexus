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
 * Indicates incompatibility when preparing external-process execution.
 */
public class ExternalProcessCompatibilityException extends ScriptException {

    private final String stage;
    private final String component;
    private final String reason;

    public ExternalProcessCompatibilityException(String message) {
        this(message, null, null, null, null);
    }

    public ExternalProcessCompatibilityException(String message, Throwable cause) {
        this(message, cause, null, null, null);
    }

    public ExternalProcessCompatibilityException(String message, String stage,
                                                 String component, String reason) {
        this(message, null, stage, component, reason);
    }

    public ExternalProcessCompatibilityException(String message, Throwable cause,
                                                 String stage, String component,
                                                 String reason) {
        super(message, cause);
        this.stage = stage;
        this.component = component;
        this.reason = reason;
    }

    public String getStage() {
        return stage;
    }

    public String getComponent() {
        return component;
    }

    public String getReason() {
        return reason;
    }
}
