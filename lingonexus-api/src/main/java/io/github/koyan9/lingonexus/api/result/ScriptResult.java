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
package io.github.koyan9.lingonexus.api.result;

import java.util.Map;

/**
 * 脚本执行结果
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-01-31
 */
public class ScriptResult {

    private final ExecutionStatus status;
    private final Object value;
    private final String errorMessage;
    private final Throwable throwable;
    private final long executionTime;
    private final Map<String, Object> metadata;

    public ScriptResult(ExecutionStatus status, Object value, String errorMessage, Throwable throwable, long executionTime, Map<String, Object> metadata) {
        this.status = status;
        this.value = value;
        this.errorMessage = errorMessage == null ? throwable == null ? null : throwable.getMessage() : errorMessage;
        this.throwable = throwable;
        this.executionTime = executionTime;
        this.metadata = metadata;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public Object getValue() {
        return value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public boolean isSuccess() {
        return status == ExecutionStatus.SUCCESS;
    }

    public boolean isFailure() {
        return status != ExecutionStatus.SUCCESS;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public static ScriptResult success(Object value) {
        return new ScriptResult(ExecutionStatus.SUCCESS, value, null, null, 0, null);
    }

    public static ScriptResult success(Object value, long executionTime) {
        return new ScriptResult(ExecutionStatus.SUCCESS, value, null, null, executionTime, null);
    }

    public static ScriptResult success(Object value, long executionTime, Map<String, Object> metadata) {
        return new ScriptResult(ExecutionStatus.SUCCESS, value, null, null, executionTime, metadata);
    }

    public static ScriptResult of(ExecutionStatus status, Object value, Throwable throwable, long executionTime, Map<String, Object> metadata) {
        return ScriptResult.of(status, value, null, throwable, executionTime, metadata);
    }

    public static ScriptResult of(ExecutionStatus status, Object value, String errorMessage, Throwable throwable, long executionTime, Map<String, Object> metadata) {
        return new ScriptResult(status, value, errorMessage, throwable, executionTime, metadata);
    }

    public static ScriptResult failure(String errorMessage) {
        return new ScriptResult(ExecutionStatus.FAILURE, null, errorMessage, null,0, null);
    }

    public static ScriptResult failure(Throwable throwable) {
        return new ScriptResult(ExecutionStatus.FAILURE, null, throwable.getMessage(), throwable,0, null);
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return String.valueOf(value);
        }
        return "ScriptResult{status=" + status + ", error=" + errorMessage + "}";
    }
}
