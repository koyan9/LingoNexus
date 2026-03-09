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

package io.github.koyan9.lingonexus.api.security;

/**
 * 安全策略校验结果
 *
 * @author lingonexus Team
 * @version 1.0.0
 * @since 2026-02-05
 */
public class ValidationResult {

    private final boolean valid;
    private final String reason;

    private ValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    /**
     * 创建校验成功的结果
     *
     * @return 校验成功结果
     */
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    /**
     * 创建校验失败的结果
     *
     * @param reason 失败原因
     * @return 校验失败结果
     */
    public static ValidationResult failure(String reason) {
        return new ValidationResult(false, reason);
    }

    /**
     * 是否校验通过
     *
     * @return true 表示通过，false 表示失败
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * 获取失败原因
     *
     * @return 失败原因，如果校验通过则为 null
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String toString() {
        return valid ? "ValidationResult{valid=true}" : "ValidationResult{valid=false, reason='" + reason + "'}";
    }
}
